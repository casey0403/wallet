package com.wannabe.wallet.presentation.model.response

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TransactionResponse(
    val transactionId: String,
    val walletId: String,
    val type: String,
    val status: String,
    val amount: BigDecimal,
    val currency: String,
    val balance: BigDecimal,
    val version: Long,
    val processedAt: LocalDateTime,
    val failureCode: String? = null,
    val failureMessage: String? = null,
)

data class TransactionsResponse(
    val transactions: List<TransactionResponse>,
)
