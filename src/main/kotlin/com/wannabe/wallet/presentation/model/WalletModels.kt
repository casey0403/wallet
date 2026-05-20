package com.wannabe.wallet.presentation.model

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.math.BigDecimal
import java.time.LocalDateTime

data class WithdrawalRequest(
    @field:DecimalMin(value = "0.0001")
    val amount: BigDecimal,

    @field:NotBlank
    val transactionId: String,

    @field:Pattern(regexp = "^[A-Z]{3}$")
    val currency: String = "KRW",
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TransactionResponse(
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

data class TransactionsResponse(
    val transactions: List<TransactionResponse>,
)

data class ErrorResponse<T>(
    val code: String,
    val message: String,
    val data: T? = null,
)
