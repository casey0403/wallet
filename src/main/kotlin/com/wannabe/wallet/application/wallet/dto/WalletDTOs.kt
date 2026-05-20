package com.wannabe.wallet.application.wallet.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class TransactionDTO(
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

data class WithdrawalResultDTO(
    val httpStatus: Int,
    val body: TransactionDTO,
)
