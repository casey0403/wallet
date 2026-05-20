package com.wannabe.wallet.domain.wallet.model

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class WalletTransaction(
    val id: Long = 0,
    val transactionId: String,
    val wallet: Wallet,
    val idempotencyRequest: IdempotencyRequest,
    val type: TransactionType,
    val status: TransactionStatus,
    val money: Money,
    val balanceBefore: Money,
    val balanceAfter: Money,
    val walletVersionBefore: Long,
    val walletVersionAfter: Long,
    val failureCode: String? = null,
    val failureMessage: String? = null,
    val processedAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS),
)

enum class TransactionType {
    WITHDRAWAL,
}

enum class TransactionStatus {
    SUCCESS,
    FAILED,
}
