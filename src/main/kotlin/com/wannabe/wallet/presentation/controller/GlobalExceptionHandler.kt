package com.wannabe.wallet.presentation.controller

import com.wannabe.wallet.domain.wallet.WalletException
import com.wannabe.wallet.presentation.model.ErrorResponse
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
            .status(exception.code.status)
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
}
