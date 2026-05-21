package com.wannabe.wallet.presentation.controller

import com.wannabe.wallet.application.wallet.assembler.WalletTransactionHistoryAssembler
import com.wannabe.wallet.application.wallet.assembler.WalletWithdrawalAssembler
import com.wannabe.wallet.presentation.model.request.toTransactionTypeFilter
import com.wannabe.wallet.presentation.model.response.CommonResponse
import com.wannabe.wallet.presentation.model.response.TransactionsResponse
import com.wannabe.wallet.presentation.model.request.WithdrawalRequest
import com.wannabe.wallet.presentation.model.response.toErrorDetailResponse
import com.wannabe.wallet.presentation.model.response.toHttpStatus
import com.wannabe.wallet.presentation.model.response.toResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/wallets")
class WalletController(
    private val walletWithdrawalAssembler: WalletWithdrawalAssembler,
    private val walletTransactionHistoryAssembler: WalletTransactionHistoryAssembler,
) {
    @PostMapping("/{walletId}/withdrawals")
    fun withdraw(
        @PathVariable walletId: String,
        @Valid @RequestBody request: WithdrawalRequest,
    ): ResponseEntity<CommonResponse<*>> {
        val response = walletWithdrawalAssembler.withdraw(
            walletId = walletId,
            amount = request.amount,
            currency = request.currency,
            transactionId = request.transactionId,
        )
        val body = response.body.toResponse()
        val httpStatus = response.body.toHttpStatus()
        if (httpStatus.is2xxSuccessful) {
            return ResponseEntity
                .status(httpStatus)
                .body(CommonResponse.success(data = body))
        }
        return ResponseEntity
            .status(httpStatus)
            .body(
                CommonResponse.failed(
                    status = httpStatus.value(),
                    data = response.body.toErrorDetailResponse(),
                ),
            )
    }

    @GetMapping("/{walletId}/transactions")
    fun getTransactions(
        @PathVariable walletId: String,
        @RequestParam(required = false) transactionType: String?,
    ): CommonResponse<TransactionsResponse> {
        val response = TransactionsResponse(
            transactions = walletTransactionHistoryAssembler.getTransactions(
                    walletId = walletId,
                    transactionType = transactionType.toTransactionTypeFilter(),
                )
                .map { it.toResponse() },
        )
        return CommonResponse.success(
            data = response,
        )
    }
}
