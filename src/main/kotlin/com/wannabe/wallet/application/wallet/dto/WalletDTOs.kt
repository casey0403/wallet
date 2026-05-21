package com.wannabe.wallet.application.wallet.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.wannabe.wallet.domain.wallet.enums.IdempotencyStatus
import com.wannabe.wallet.domain.wallet.error.WalletErrorCode
import com.wannabe.wallet.domain.wallet.exception.WalletException
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
    val withdrawalAmount: BigDecimal,
    val currency: String,
    val balance: BigDecimal,
    val version: Long,
    val withdrawalDate: LocalDateTime,
    val failureCode: String? = null,
    val failureMessage: String? = null,
)

data class WithdrawalResultDTO(
    val httpStatus: Int,
    val body: TransactionDTO,
)

fun WalletTransaction.toTransactionDTO(): TransactionDTO {
    return TransactionDTO(
        transactionId = transactionId,
        walletId = wallet.walletId,
        type = type.name,
        status = status.name,
        withdrawalAmount = money.amount,
        currency = money.currency,
        balance = balanceAfter.amount,
        version = walletVersionAfter,
        withdrawalDate = processedAt,
    )
}

fun WalletWithdrawalResult.toTransactionDTO(): TransactionDTO {
    return TransactionDTO(
        transactionId = transactionId,
        walletId = walletId,
        type = type.name,
        status = status.name,
        withdrawalAmount = money.amount,
        currency = money.currency,
        balance = balance.amount,
        version = walletVersion,
        withdrawalDate = processedAt,
        failureCode = failureCode?.name,
        failureMessage = failureCode?.message,
    )
}

fun IdempotencyRequest.toWithdrawalResultDTO(
    requestHash: String,
    objectMapper: ObjectMapper,
): WithdrawalResultDTO {
    val storedHttpStatus = httpStatus
    val storedResponseSnapshot = responseSnapshot

    if (this.requestHash != requestHash) {
        throw WalletException(WalletErrorCode.IDEMPOTENCY_KEY_CONFLICT)
    }

    if (
        status != IdempotencyStatus.COMPLETED ||
        storedHttpStatus == null ||
        storedResponseSnapshot == null
    ) {
        throw WalletException(WalletErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS)
    }

    return WithdrawalResultDTO(
        httpStatus = storedHttpStatus,
        body = objectMapper.readValue(storedResponseSnapshot, TransactionDTO::class.java),
    )
}

fun WithdrawalResultDTO.toResponseSnapshot(objectMapper: ObjectMapper): String {
    return objectMapper.writeValueAsString(body)
}
