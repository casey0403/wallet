package com.wannabe.wallet.presentation.model.request

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.math.BigDecimal

data class WithdrawalRequest(
    @field:DecimalMin(value = "0.0001")
    val amount: BigDecimal,

    @field:NotBlank
    val transactionId: String,

    @field:Pattern(regexp = "^[A-Z]{3}$")
    val currency: String = "KRW",
)
