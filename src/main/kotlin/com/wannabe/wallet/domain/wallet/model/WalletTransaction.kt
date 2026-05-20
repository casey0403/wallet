package com.wannabe.wallet.domain.wallet.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Entity
@Table(
    name = "wallet_transactions",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_wallet_transactions_wallet_transaction_id", columnNames = ["wallet_id", "transaction_id"]),
        UniqueConstraint(name = "uk_wallet_transactions_idempotency_request_id", columnNames = ["idempotency_request_id"]),
    ],
)
class WalletTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "transaction_id", nullable = false, length = 128)
    val transactionId: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    val wallet: Wallet,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idempotency_request_id", nullable = false)
    val idempotencyRequest: IdempotencyRequest,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    val type: TransactionType,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: TransactionStatus,

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    val amount: BigDecimal,

    @Column(name = "currency", nullable = false, length = 3)
    val currency: String,

    @Column(name = "balance_before", nullable = false, precision = 19, scale = 4)
    val balanceBefore: BigDecimal,

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    val balanceAfter: BigDecimal,

    @Column(name = "wallet_version_before", nullable = false)
    val walletVersionBefore: Long,

    @Column(name = "wallet_version_after", nullable = false)
    val walletVersionAfter: Long,

    @Column(name = "failure_code", length = 50)
    val failureCode: String? = null,

    @Column(name = "failure_message", length = 255)
    val failureMessage: String? = null,

    @Column(name = "processed_at", nullable = false)
    val processedAt: LocalDateTime = nowMicros(),
)

enum class TransactionType {
    WITHDRAWAL,
}

enum class TransactionStatus {
    SUCCESS,
    FAILED,
}

private fun nowMicros(): LocalDateTime {
    return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
}
