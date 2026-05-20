package com.wannabe.wallet.presentation.model.response

data class ErrorResponse<T>(
    val code: String,
    val message: String,
    val data: T? = null,
)
