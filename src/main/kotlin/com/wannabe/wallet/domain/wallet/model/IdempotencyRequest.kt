package com.wannabe.wallet.domain.wallet.model

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class IdempotencyRequest(
    val id: Long = 0,
    val wallet: Wallet,
    val idempotencyKey: String,
    val requestHash: String,
    val operationType: OperationType,
    val money: Money,
    var status: IdempotencyStatus = IdempotencyStatus.PROCESSING,
    var httpStatus: Int? = null,
    var responseSnapshot: String? = null,
    val createdAt: LocalDateTime? = null,
    var completedAt: LocalDateTime? = null,
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
