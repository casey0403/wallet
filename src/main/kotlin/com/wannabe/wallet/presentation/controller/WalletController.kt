package com.wannabe.wallet.presentation.controller

import com.wannabe.wallet.application.wallet.command.WalletCommandService
import com.wannabe.wallet.application.wallet.query.WalletQueryService
import com.wannabe.wallet.application.wallet.dto.TransactionResult
import com.wannabe.wallet.presentation.model.ErrorResponse
import com.wannabe.wallet.presentation.model.TransactionResponse
import com.wannabe.wallet.presentation.model.TransactionsResponse
import com.wannabe.wallet.presentation.model.WithdrawalRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/wallets")
class WalletController(
    private val walletCommandService: WalletCommandService,
    private val walletQueryService: WalletQueryService,
) {
    @PostMapping("/{walletId}/withdrawals")
    fun withdraw(
        @PathVariable walletId: String,
        @Valid @RequestBody request: WithdrawalRequest,
    ): ResponseEntity<Any> {
        val response = walletCommandService.withdraw(walletId, request.amount, request.currency, request.transactionId)
        val body = response.body.toResponse()
        if (response.httpStatus in 200..299) {
            return ResponseEntity.status(response.httpStatus).body(body)
        }
        return ResponseEntity.status(response.httpStatus).body(body.toErrorResponse())
    }

    @GetMapping("/{walletId}/transactions")
    fun getTransactions(@PathVariable walletId: String): TransactionsResponse {
        return TransactionsResponse(walletQueryService.getTransactions(walletId).map { it.toResponse() })
    }

    private fun TransactionResult.toResponse(): TransactionResponse {
        return TransactionResponse(
            transactionId = transactionId,
            walletId = walletId,
            type = type,
            status = status,
            withdrawalAmount = withdrawalAmount,
            currency = currency,
            balance = balance,
            version = version,
            withdrawalDate = withdrawalDate,
            failureCode = failureCode,
            failureMessage = failureMessage,
        )
    }

    private fun TransactionResponse.toErrorResponse(): ErrorResponse<TransactionResponse> {
        return ErrorResponse(
            code = failureCode ?: "WITHDRAWAL_FAILED",
            message = failureMessage ?: "Withdrawal failed",
            data = this,
        )
    }
}
