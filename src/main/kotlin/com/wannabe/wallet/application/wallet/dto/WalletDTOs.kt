package com.wannabe.wallet.application.wallet.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.wannabe.wallet.domain.wallet.exception.IdempotencyKeyConflictException
import com.wannabe.wallet.domain.wallet.exception.IdempotencyRequestInProgressException
import com.wannabe.wallet.domain.wallet.enums.IdempotencyStatus
import com.wannabe.wallet.domain.wallet.model.IdempotencyRequest
import com.wannabe.wallet.domain.wallet.model.WalletTransaction
import com.wannabe.wallet.domain.wallet.model.WalletWithdrawalResult
import java.math.BigDecimal
import java.time.LocalDateTime

data class TransactionDTO(
    val transactionId: String,
    val walletId: String,
    val type: String,
    val status: String,
    val amount: BigDecimal,
    val currency: String,
    val balance: BigDecimal,
    val version: Long,
    val processedAt: LocalDateTime,
    val failureCode: String? = null,
    val failureMessage: String? = null,
)

data class WithdrawalResultDTO(
    val body: TransactionDTO,
)

fun WalletTransaction.toTransactionDTO(): TransactionDTO {
    return TransactionDTO(
        transactionId = transactionId,
        walletId = wallet.walletId,
        type = type.name,
        status = status.name,
        amount = money.amount,
        currency = money.currency,
        balance = balanceAfter.amount,
        version = walletVersionAfter,
        processedAt = processedAt,
    )
}

fun WalletWithdrawalResult.toTransactionDTO(): TransactionDTO {
    return TransactionDTO(
        transactionId = transactionId,
        walletId = walletId,
        type = type.name,
        status = status.name,
        amount = money.amount,
        currency = money.currency,
        balance = balance.amount,
        version = walletVersion,
        processedAt = processedAt,
        failureCode = failureCode?.name,
        failureMessage = failureCode?.message,
    )
}

fun IdempotencyRequest.toWithdrawalResultDTO(
    requestHash: String,
    objectMapper: ObjectMapper,
): WithdrawalResultDTO {
    val storedResponseSnapshot = responseSnapshot

    if (this.requestHash != requestHash) {
        throw IdempotencyKeyConflictException()
    }

    if (
        status != IdempotencyStatus.COMPLETED ||
        storedResponseSnapshot == null
    ) {
        throw IdempotencyRequestInProgressException()
    }

    return WithdrawalResultDTO(
        body = objectMapper.readValue(storedResponseSnapshot, TransactionDTO::class.java),
    )
}

fun WithdrawalResultDTO.toResponseSnapshot(objectMapper: ObjectMapper): String {
    return objectMapper.writeValueAsString(body)
}
