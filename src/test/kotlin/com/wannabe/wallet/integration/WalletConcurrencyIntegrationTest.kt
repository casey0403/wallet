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
import org.junit.jupiter.api.TestReporter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Import(TestcontainersConfiguration::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WalletConcurrencyIntegrationTest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val walletRepository: WalletJpaRepository,
    private val idempotencyRequestRepository: IdempotencyRequestJpaRepository,
    private val walletTransactionRepository: WalletTransactionJpaRepository,
    private val jdbcTemplate: JdbcTemplate,
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
    fun `unsafe read-modify-write loses balance integrity`(testReporter: TestReporter) {
        val threadCount = 100
        val withdrawalAmount = BigDecimal("10000.0000")
        val executor = Executors.newFixedThreadPool(threadCount)
        val startLatch = CountDownLatch(1)
        val allThreadsReadLatch = CountDownLatch(threadCount)
        val writeLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val successfulCount = AtomicInteger(0)

        repeat(threadCount) {
            executor.submit {
                try {
                    startLatch.await()
                    val currentBalance = jdbcTemplate.queryForObject(
                        "SELECT balance FROM wallets WHERE id = ?",
                        BigDecimal::class.java,
                        walletId,
                    ) ?: BigDecimal.ZERO
                    val nextBalance = currentBalance - withdrawalAmount
                    if (currentBalance >= withdrawalAmount) {
                        successfulCount.incrementAndGet()
                    }

                    allThreadsReadLatch.countDown()
                    writeLatch.await()

                    jdbcTemplate.update(
                        "UPDATE wallets SET balance = ?, version = version + 1 WHERE id = ?",
                        nextBalance,
                        walletId,
                    )
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        assertThat(allThreadsReadLatch.await(30, TimeUnit.SECONDS)).isTrue()
        writeLatch.countDown()
        assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue()
        executor.shutdown()

        val wallet = walletRepository.findById(walletId).orElseThrow()
        val requestedTotal = BigDecimal(successfulCount.get()) * withdrawalAmount

        testReporter.publishEntry(
            mapOf(
                "case" to "before concurrency control",
                "threadCount" to threadCount.toString(),
                "withdrawalAmount" to withdrawalAmount.toPlainString(),
                "successfulCount" to successfulCount.get().toString(),
                "requestedTotal" to requestedTotal.toPlainString(),
                "finalBalance" to wallet.balance.toPlainString(),
                "walletVersion" to wallet.version.toString(),
                "result" to "lost update reproduced",
            ),
        )
        println(
            "[BEFORE] threads=$threadCount amount=$withdrawalAmount " +
                "successful=${successfulCount.get()} requestedTotal=$requestedTotal " +
                "finalBalance=${wallet.balance} version=${wallet.version} result=LOST_UPDATE",
        )

        assertThat(successfulCount.get()).isEqualTo(100)
        assertThat(requestedTotal).isEqualByComparingTo("1000000.0000")
        assertThat(wallet.balance).isEqualByComparingTo("490000.0000")
    }

    @Test
    fun `same wallet withdrawals are processed sequentially without overdraft`(testReporter: TestReporter) {
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
        val withdrawnTotal = BigDecimal(successfulCount) * withdrawalAmount

        testReporter.publishEntry(
            mapOf(
                "case" to "after concurrency control",
                "threadCount" to threadCount.toString(),
                "withdrawalAmount" to withdrawalAmount.toPlainString(),
                "successfulCount" to successfulCount.toString(),
                "failedCount" to failedCount.toString(),
                "withdrawnTotal" to withdrawnTotal.toPlainString(),
                "finalBalance" to wallet.balance.toPlainString(),
                "walletVersion" to wallet.version.toString(),
                "result" to "balance integrity preserved",
            ),
        )
        println(
            "[AFTER] threads=$threadCount amount=$withdrawalAmount " +
                "success=$successfulCount failed=$failedCount withdrawnTotal=$withdrawnTotal " +
                "finalBalance=${wallet.balance} version=${wallet.version} result=OK",
        )

        assertThat(wallet.balance).isZero()
        assertThat(successfulCount).isEqualTo(50)
        assertThat(failedCount).isEqualTo(50)
        assertThat(withdrawnTotal).isEqualByComparingTo("500000.0000")
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
