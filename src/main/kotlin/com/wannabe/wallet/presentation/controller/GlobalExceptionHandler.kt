package com.wannabe.wallet.presentation.controller

import com.wannabe.wallet.domain.wallet.exception.WalletException
import com.wannabe.wallet.domain.wallet.exception.WalletErrorCode
import com.wannabe.wallet.presentation.model.response.ErrorResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(WalletException::class)
    fun handleWalletException(exception: WalletException): ResponseEntity<ErrorResponse<Unit>> {
        return ResponseEntity
            .status(exception.code.httpStatus())
            .body(
                ErrorResponse(
                    code = exception.code.name,
                    message = exception.message,
                ),
            )
    }

    @ExceptionHandler(MethodArgumentNotValidException::class, ConstraintViolationException::class)
    fun handleValidationException(exception: Exception): ResponseEntity<ErrorResponse<Unit>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                ErrorResponse(
                    code = "INVALID_REQUEST",
                    message = exception.message ?: "Invalid request",
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
