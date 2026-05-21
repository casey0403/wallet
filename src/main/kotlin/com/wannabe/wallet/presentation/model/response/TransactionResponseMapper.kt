package com.wannabe.wallet.presentation.model.response

import com.wannabe.wallet.application.wallet.dto.TransactionDTO
import com.wannabe.wallet.domain.wallet.error.WalletErrorCode
import com.wannabe.wallet.presentation.error.toHttpStatus
import org.springframework.http.HttpStatus

fun TransactionDTO.toResponse(): TransactionResponse {
    return TransactionResponse(
        transactionId = transactionId,
        walletId = walletId,
        type = type,
        status = status,
        amount = amount,
        currency = currency,
        balance = balance,
        version = version,
        processedAt = processedAt,
        failureCode = failureCode,
        failureMessage = failureMessage,
    )
}

fun TransactionDTO.toErrorDetailResponse(): ErrorDetailResponse {
    return ErrorDetailResponse(
        errorCode = failureCode ?: "WITHDRAWAL_FAILED",
        message = failureMessage ?: "Withdrawal failed",
    )
}

fun TransactionDTO.toHttpStatus(): HttpStatus {
    val code = failureCode ?: return HttpStatus.OK
    return WalletErrorCode.valueOf(code).toHttpStatus()
}
