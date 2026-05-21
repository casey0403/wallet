package com.wannabe.wallet.presentation.error

import org.springframework.http.HttpStatus

enum class PresentationErrorCode(
    override val message: String,
    val httpStatus: HttpStatus,
) : ErrorCode {
    INVALID_TRANSACTION_TYPE("Invalid transactionType", HttpStatus.BAD_REQUEST),
    ;

    override val code: String
        get() = name
}
