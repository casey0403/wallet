package com.wannabe.wallet.presentation.model.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class WithdrawalRequest(
    @field:Min(value = 1)
    val amount: Long,

    @field:NotBlank
    val transactionId: String,

    // TODO: 다통화 지원 시 currency 필드 추가
    val currency: String = "KRW",
)
