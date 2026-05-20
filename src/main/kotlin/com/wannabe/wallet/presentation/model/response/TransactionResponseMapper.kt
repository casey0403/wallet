package com.wannabe.wallet.presentation.model.response

import com.wannabe.wallet.application.wallet.dto.TransactionDTO

fun TransactionDTO.toResponse(): TransactionResponse {
    return TransactionResponse(
        transactionId = transactionId,
        walletId = walletId,
        type = type,
        status = status,
        withdrawalAmount = withdrawalAmount,
        currency = currency,
        balance = balance,
        version = version,
        withdrawalDate = withdrawalDate,
        failureCode = failureCode,
        failureMessage = failureMessage,
    )
}

fun TransactionResponse.toErrorResponse(): ErrorResponse<TransactionResponse> {
    return ErrorResponse(
        code = failureCode ?: "WITHDRAWAL_FAILED",
        message = failureMessage ?: "Withdrawal failed",
        data = this,
    )
}
