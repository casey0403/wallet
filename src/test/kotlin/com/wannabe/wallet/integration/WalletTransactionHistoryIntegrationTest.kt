package com.wannabe.wallet.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.wannabe.wallet.TestcontainersConfiguration
import com.wannabe.wallet.domain.wallet.enums.OperationType
import com.wannabe.wallet.domain.wallet.enums.TransactionStatus
import com.wannabe.wallet.domain.wallet.enums.TransactionType
import com.wannabe.wallet.domain.wallet.enums.WalletStatus
import com.wannabe.wallet.infrastructure.jpa.IdempotencyRequestJpaRepository
import com.wannabe.wallet.infrastructure.jpa.WalletJpaRepository
import com.wannabe.wallet.infrastructure.jpa.WalletTransactionJpaRepository
import com.wannabe.wallet.infrastructure.jpa.entity.IdempotencyRequestJPAEntity
import com.wannabe.wallet.infrastructure.jpa.entity.WalletJPAEntity
import com.wannabe.wallet.infrastructure.jpa.entity.WalletTransactionJPAEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDateTime

@Import(TestcontainersConfiguration::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WalletTransactionHistoryIntegrationTest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val walletRepository: WalletJpaRepository,
    private val idempotencyRequestRepository: IdempotencyRequestJpaRepository,
    private val walletTransactionRepository: WalletTransactionJpaRepository,
) {
    private val walletId = "history-wallet"

    @BeforeEach
    fun setUp() {
        walletTransactionRepository.deleteAll()
        idempotencyRequestRepository.deleteAll()
        walletRepository.deleteAll()
        walletRepository.save(
            WalletJPAEntity(
                walletId = walletId,
                balance = BigDecimal("505000.0000"),
                walletStatus = WalletStatus.ACTIVE,
            ),
        )

        saveTransaction(
            transactionId = "HISTORY-DEPOSIT",
            type = TransactionType.DEPOSIT,
            amount = BigDecimal("10000.0000"),
            balanceBefore = BigDecimal("500000.0000"),
            balanceAfter = BigDecimal("510000.0000"),
            walletVersionBefore = 0,
            walletVersionAfter = 1,
            processedAt = LocalDateTime.parse("2026-05-21T10:00:00"),
        )
        saveTransaction(
            transactionId = "HISTORY-WITHDRAWAL",
            type = TransactionType.WITHDRAWAL,
            amount = BigDecimal("5000.0000"),
            balanceBefore = BigDecimal("510000.0000"),
            balanceAfter = BigDecimal("505000.0000"),
            walletVersionBefore = 1,
            walletVersionAfter = 2,
            processedAt = LocalDateTime.parse("2026-05-21T11:00:00"),
        )
    }

    @Test
    fun `transaction history without transaction type returns all transactions using common response fields`() {
        val response = restTemplate.getForEntity(
            "/api/v1/wallets/$walletId/transactions",
            String::class.java,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val transactions = response.bodyAsJson()["data"]["transactions"]
        assertThat(transactions).hasSize(2)
        assertThat(transactions.map { it["type"].asText() }).containsExactly("WITHDRAWAL", "DEPOSIT")
        assertThat(transactions[0].has("amount")).isTrue()
        assertThat(transactions[0].has("processedAt")).isTrue()
        assertThat(transactions[0].has("withdrawalAmount")).isFalse()
        assertThat(transactions[0].has("withdrawalDate")).isFalse()
    }

    @Test
    fun `transaction history filters by transaction type case insensitively`() {
        val depositResponse = restTemplate.getForEntity(
            "/api/v1/wallets/$walletId/transactions?transactionType=Deposit",
            String::class.java,
        )
        val withdrawalResponse = restTemplate.getForEntity(
            "/api/v1/wallets/$walletId/transactions?transactionType=WITHDRAW",
            String::class.java,
        )

        assertThat(depositResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(depositResponse.bodyAsJson()["data"]["transactions"].map { it["type"].asText() })
            .containsExactly("DEPOSIT")
        assertThat(withdrawalResponse.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(withdrawalResponse.bodyAsJson()["data"]["transactions"].map { it["type"].asText() })
            .containsExactly("WITHDRAWAL")
    }

    @Test
    fun `transaction history rejects all empty and unsupported transaction types`() {
        listOf("ALL", "", "UNKNOWN").forEach { transactionType ->
            val response = restTemplate.getForEntity(
                "/api/v1/wallets/$walletId/transactions?transactionType=$transactionType",
                String::class.java,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.bodyAsJson()["data"]["errorCode"].asText())
                .isEqualTo("INVALID_TRANSACTION_TYPE")
        }
    }

    private fun saveTransaction(
        transactionId: String,
        type: TransactionType,
        amount: BigDecimal,
        balanceBefore: BigDecimal,
        balanceAfter: BigDecimal,
        walletVersionBefore: Long,
        walletVersionAfter: Long,
        processedAt: LocalDateTime,
    ) {
        val wallet = walletRepository.findById(walletId).orElseThrow()
        val idempotencyRequest = idempotencyRequestRepository.save(
            IdempotencyRequestJPAEntity(
                wallet = wallet,
                idempotencyKey = transactionId,
                requestHash = transactionId.padEnd(64, '0').take(64),
                operationType = type.toOperationType(),
                amount = amount,
                currency = "KRW",
                expiresAt = processedAt.plusDays(7),
            ),
        )

        walletTransactionRepository.save(
            WalletTransactionJPAEntity(
                transactionId = transactionId,
                wallet = wallet,
                idempotencyRequest = idempotencyRequest,
                type = type,
                status = TransactionStatus.SUCCESS,
                amount = amount,
                currency = "KRW",
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                walletVersionBefore = walletVersionBefore,
                walletVersionAfter = walletVersionAfter,
                processedAt = processedAt,
            ),
        )
    }

    private fun TransactionType.toOperationType(): OperationType {
        return when (this) {
            TransactionType.DEPOSIT -> OperationType.DEPOSIT
            TransactionType.WITHDRAWAL -> OperationType.WITHDRAWAL
        }
    }

    private fun org.springframework.http.ResponseEntity<String>.bodyAsJson(): JsonNode {
        return objectMapper.readTree(body)
    }
}
