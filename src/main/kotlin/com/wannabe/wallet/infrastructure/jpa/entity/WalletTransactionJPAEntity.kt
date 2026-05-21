package com.wannabe.wallet.infrastructure.jpa.entity

import com.wannabe.wallet.domain.wallet.vo.Money
import com.wannabe.wallet.domain.wallet.enums.TransactionStatus
import com.wannabe.wallet.domain.wallet.enums.TransactionType
import com.wannabe.wallet.domain.wallet.model.WalletTransaction
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
class WalletTransactionJPAEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "transaction_id", nullable = false, length = 128)
    val transactionId: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    val wallet: WalletJPAEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idempotency_request_id", nullable = false)
    val idempotencyRequest: IdempotencyRequestJPAEntity,

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

    @Column(name = "processed_at", nullable = false)
    val processedAt: LocalDateTime = nowMicros(),
) {
    fun toDomain(): WalletTransaction {
        return WalletTransaction(
            id = id,
            transactionId = transactionId,
            wallet = wallet.toDomain(),
            idempotencyRequest = idempotencyRequest.toDomain(),
            type = type,
            status = status,
            money = Money(
                amount = amount,
                currency = currency,
            ),
            balanceBefore = Money(
                amount = balanceBefore,
                currency = currency,
            ),
            balanceAfter = Money(
                amount = balanceAfter,
                currency = currency,
            ),
            walletVersionBefore = walletVersionBefore,
            walletVersionAfter = walletVersionAfter,
            processedAt = processedAt,
        )
    }
}

private fun nowMicros(): LocalDateTime {
    return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
}

fun WalletTransaction.toJPAEntity(
    walletJPAEntity: WalletJPAEntity,
    idempotencyRequestJPAEntity: IdempotencyRequestJPAEntity,
): WalletTransactionJPAEntity {
    return WalletTransactionJPAEntity(
        id = id,
        transactionId = transactionId,
        wallet = walletJPAEntity,
        idempotencyRequest = idempotencyRequestJPAEntity,
        type = type,
        status = status,
        amount = money.amount,
        currency = money.currency,
        balanceBefore = balanceBefore.amount,
        balanceAfter = balanceAfter.amount,
        walletVersionBefore = walletVersionBefore,
        walletVersionAfter = walletVersionAfter,
        processedAt = processedAt,
    )
}
