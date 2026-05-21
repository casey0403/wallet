package com.wannabe.wallet.presentation.controller

import com.wannabe.wallet.domain.wallet.exception.WalletException
import com.wannabe.wallet.domain.wallet.error.WalletErrorCode
import com.wannabe.wallet.presentation.model.response.CommonResponse
import com.wannabe.wallet.presentation.model.response.ErrorDetailResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(WalletException::class)
    fun handleWalletException(exception: WalletException): ResponseEntity<CommonResponse<ErrorDetailResponse>> {
        val status = exception.code.httpStatus()
        return ResponseEntity
            .status(status)
            .body(
                CommonResponse.failed(
                    status = status.value(),
                    data = ErrorDetailResponse(
                        errorCode = exception.code.name,
                        message = exception.message,
                    ),
                ),
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class, ConstraintViolationException::class)
    fun handleValidationException(exception: Exception): ResponseEntity<CommonResponse<ErrorDetailResponse>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                CommonResponse.failed(
                    status = HttpStatus.BAD_REQUEST.value(),
                    data = ErrorDetailResponse(
                        errorCode = "INVALID_REQUEST",
                        message = exception.message ?: "Invalid request",
                    ),
                ),
            )
    }

    private fun WalletErrorCode.httpStatus(): HttpStatus {
        return when (this) {
            WalletErrorCode.WALLET_NOT_FOUND -> HttpStatus.NOT_FOUND
            WalletErrorCode.CURRENCY_MISMATCH,
            WalletErrorCode.INVALID_AMOUNT
            -> HttpStatus.BAD_REQUEST
            WalletErrorCode.INSUFFICIENT_BALANCE,
            WalletErrorCode.IDEMPOTENCY_KEY_CONFLICT,
            WalletErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS,
            WalletErrorCode.WALLET_BUSY,
            WalletErrorCode.WALLET_CONCURRENT_MODIFICATION
            -> HttpStatus.CONFLICT
        }
    }
}
