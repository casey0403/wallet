package com.wannabe.wallet.presentation.controller

import com.wannabe.wallet.domain.wallet.exception.AbstractWalletException
import com.wannabe.wallet.presentation.error.toHttpStatus
import com.wannabe.wallet.presentation.exception.PresentationException
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
    @ExceptionHandler(PresentationException::class)
    fun handlePresentationException(exception: PresentationException): ResponseEntity<CommonResponse<ErrorDetailResponse>> {
        val status = exception.code.httpStatus
        return ResponseEntity
            .status(status)
            .body(
                CommonResponse.failed(
                    status = status.value(),
                    data = ErrorDetailResponse(
                        errorCode = exception.code.name,
                        message = exception.code.message,
                    ),
                ),
            )
    }

    @ExceptionHandler(AbstractWalletException::class)
    fun handleWalletException(exception: AbstractWalletException): ResponseEntity<CommonResponse<ErrorDetailResponse>> {
        val status = exception.code.toHttpStatus()
        return ResponseEntity
            .status(status)
            .body(
                CommonResponse.failed(
                    status = status.value(),
                    data = ErrorDetailResponse(
                        errorCode = exception.code.name,
                        message = exception.code.message,
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
}
