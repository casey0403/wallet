package com.wannabe.wallet.domain.wallet.model

import com.wannabe.wallet.domain.wallet.enums.IdempotencyStatus
import com.wannabe.wallet.domain.wallet.enums.OperationType
import com.wannabe.wallet.domain.wallet.vo.Money
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
    var responseSnapshot: String? = null,
    val createdAt: LocalDateTime? = null,
    var completedAt: LocalDateTime? = null,
    val expiresAt: LocalDateTime,
) {
    fun complete(responseSnapshot: String) {
        this.status = IdempotencyStatus.COMPLETED
        this.responseSnapshot = responseSnapshot
        this.completedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS)
    }
}
