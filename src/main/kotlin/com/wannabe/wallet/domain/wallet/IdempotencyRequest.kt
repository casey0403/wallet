package com.wannabe.wallet.domain.wallet

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
    name = "idempotency_requests",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_idempotency_requests_wallet_key", columnNames = ["wallet_id", "idempotency_key"]),
    ],
)
class IdempotencyRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    val wallet: Wallet,

    @Column(name = "idempotency_key", nullable = false, length = 128)
    val idempotencyKey: String,

    @Column(name = "request_hash", nullable = false, length = 64)
    val requestHash: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 30)
    val operationType: OperationType,

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    val amount: BigDecimal,

    @Column(name = "currency", nullable = false, length = 3)
    val currency: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: IdempotencyStatus = IdempotencyStatus.PROCESSING,

    @Column(name = "http_status")
    var httpStatus: Int? = null,

    @Column(name = "response_snapshot", columnDefinition = "LONGTEXT")
    var responseSnapshot: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    val createdAt: LocalDateTime? = null,

    @Column(name = "completed_at")
    var completedAt: LocalDateTime? = null,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,
) {
    fun complete(httpStatus: Int, responseSnapshot: String) {
        this.status = IdempotencyStatus.COMPLETED
        this.httpStatus = httpStatus
        this.responseSnapshot = responseSnapshot
        this.completedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
    }
}

enum class OperationType {
    WITHDRAWAL,
}

enum class IdempotencyStatus {
    PROCESSING,
    COMPLETED,
}
