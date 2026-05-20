package com.wannabe.wallet.integration

import com.wannabe.wallet.TestcontainersConfiguration
import com.wannabe.wallet.domain.wallet.TransactionStatus
import com.wannabe.wallet.domain.wallet.Wallet
import com.wannabe.wallet.infrastructure.jpa.IdempotencyRequestJpaRepository
import com.wannabe.wallet.infrastructure.jpa.WalletJpaRepository
import com.wannabe.wallet.infrastructure.jpa.WalletTransactionJpaRepository
import com.wannabe.wallet.presentation.model.WithdrawalRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Import(TestcontainersConfiguration::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WalletConcurrencyIntegrationTest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val walletRepository: WalletJpaRepository,
    private val idempotencyRequestRepository: IdempotencyRequestJpaRepository,
    private val walletTransactionRepository: WalletTransactionJpaRepository,
) {
    private val walletId = "concurrency-wallet"

    @BeforeEach
    fun setUp() {
        walletTransactionRepository.deleteAll()
        idempotencyRequestRepository.deleteAll()
        walletRepository.deleteAll()
        walletRepository.save(Wallet(walletId = walletId, balance = BigDecimal("500000.0000")))
    }

    @Test
    fun `same wallet withdrawals are processed sequentially without overdraft`() {
        val threadCount = 100
        val withdrawalAmount = BigDecimal("10000.0000")
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        repeat(threadCount) { index ->
            executor.submit {
                try {
                    startLatch.await()
                    restTemplate.exchange(
                        "/api/v1/wallets/$walletId/withdrawals",
                        HttpMethod.POST,
                        HttpEntity(WithdrawalRequest(withdrawalAmount, "TXN-$index")),
                        String::class.java,
                    )
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue()
        executor.shutdown()

        val wallet = walletRepository.findById(walletId).orElseThrow()
        val transactions = walletTransactionRepository.findAllByWalletWalletIdOrderByProcessedAtDescIdDesc(walletId)
        val successfulCount = transactions.count { it.status == TransactionStatus.SUCCESS }
        val failedCount = transactions.count { it.status == TransactionStatus.FAILED }

        assertThat(wallet.balance).isZero()
        assertThat(successfulCount).isEqualTo(50)
        assertThat(failedCount).isEqualTo(50)
        assertThat(BigDecimal(successfulCount) * withdrawalAmount).isEqualByComparingTo("500000.0000")
    }

    @Test
    fun `same transaction id is idempotent`() {
        val request = WithdrawalRequest(amount = BigDecimal("10000.0000"), transactionId = "TXN-IDEMPOTENT")

        val first = restTemplate.postForEntity(
            "/api/v1/wallets/$walletId/withdrawals",
            request,
            String::class.java,
        )
        val second = restTemplate.postForEntity(
            "/api/v1/wallets/$walletId/withdrawals",
            request,
            String::class.java,
        )

        val wallet = walletRepository.findById(walletId).orElseThrow()
        val transactions = walletTransactionRepository.findAllByWalletWalletIdOrderByProcessedAtDescIdDesc(walletId)

        assertThat(first.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(second.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(first.body).isEqualTo(second.body)
        assertThat(wallet.balance).isEqualByComparingTo("490000.0000")
        assertThat(transactions).hasSize(1)
    }

    @Test
    fun `same failed transaction id returns same failed response without changing balance`() {
        val request = WithdrawalRequest(amount = BigDecimal("600000.0000"), transactionId = "TXN-IDEMPOTENT-FAILED")

        val first = restTemplate.postForEntity(
            "/api/v1/wallets/$walletId/withdrawals",
            request,
            String::class.java,
        )
        val second = restTemplate.postForEntity(
            "/api/v1/wallets/$walletId/withdrawals",
            request,
            String::class.java,
        )

        val wallet = walletRepository.findById(walletId).orElseThrow()
        val transactions = walletTransactionRepository.findAllByWalletWalletIdOrderByProcessedAtDescIdDesc(walletId)

        assertThat(first.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(second.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(first.body).isEqualTo(second.body)
        assertThat(wallet.balance).isEqualByComparingTo("500000.0000")
        assertThat(transactions).hasSize(1)
        assertThat(transactions.single().status).isEqualTo(TransactionStatus.FAILED)
    }
}
