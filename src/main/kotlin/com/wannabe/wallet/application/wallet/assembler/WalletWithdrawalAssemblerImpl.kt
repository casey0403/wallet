package com.wannabe.wallet.application.wallet.assembler

import com.fasterxml.jackson.databind.ObjectMapper
import com.wannabe.wallet.application.common.ApplicationService
import com.wannabe.wallet.application.wallet.dto.WithdrawalResultDTO
import com.wannabe.wallet.application.wallet.dto.toResponseSnapshot
import com.wannabe.wallet.application.wallet.dto.toTransactionDTO
import com.wannabe.wallet.application.wallet.dto.toWithdrawalResultDTO
import com.wannabe.wallet.domain.wallet.error.WalletErrorCode
import com.wannabe.wallet.domain.wallet.port.WalletLock
import com.wannabe.wallet.domain.wallet.service.WalletDomainService
import com.wannabe.wallet.domain.wallet.vo.Money
import org.springframework.http.HttpStatus
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal

@ApplicationService
class WalletWithdrawalAssemblerImpl(
    private val walletLock: WalletLock,
    private val walletDomainService: WalletDomainService,
    private val objectMapper: ObjectMapper,
    private val transactionTemplate: TransactionTemplate,
) : WalletWithdrawalAssembler {
    override fun withdraw(
        walletId: String,
        amount: BigDecimal,
        currency: String,
        transactionId: String,
    ): WithdrawalResultDTO {
        val money = Money(
            amount = amount,
            currency = currency,
        )
        walletDomainService.validateWithdrawalAmount(money)

        return walletLock.execute(walletId = walletId) {
            withdrawWithLock(
                walletId = walletId,
                money = money,
                transactionId = transactionId,
            )
        }
    }

    private fun withdrawWithLock(
        walletId: String,
        money: Money,
        transactionId: String,
    ): WithdrawalResultDTO {
        val requestHash = walletDomainService.withdrawalRequestHash(
            walletId = walletId,
            money = money,
            transactionId = transactionId,
        )
        val existing = walletDomainService.findIdempotencyRequest(
            walletId = walletId,
            transactionId = transactionId,
        )

        if (existing != null) {
            return existing.toWithdrawalResultDTO(
                requestHash = requestHash,
                objectMapper = objectMapper,
            )
        }

        return transactionTemplate.execute {
            val existingInTransaction = walletDomainService.findIdempotencyRequest(
                walletId = walletId,
                transactionId = transactionId,
            )
            if (existingInTransaction != null) {
                return@execute existingInTransaction.toWithdrawalResultDTO(
                    requestHash = requestHash,
                    objectMapper = objectMapper,
                )
            }

            val (wallet, idempotencyRequest) = walletDomainService.startWithdrawal(
                walletId = walletId,
                money = money,
                transactionId = transactionId,
                requestHash = requestHash,
            )

            val withdrawalResult = walletDomainService.withdraw(
                wallet = wallet,
                idempotencyRequest = idempotencyRequest,
                money = money,
                transactionId = transactionId,
            )
            val result = WithdrawalResultDTO(
                httpStatus = withdrawalResult.failureCode?.httpStatus() ?: HttpStatus.OK.value(),
                body = withdrawalResult.toTransactionDTO(),
            )

            walletDomainService.completeIdempotencyRequest(
                idempotencyRequest = idempotencyRequest,
                httpStatus = result.httpStatus,
                responseSnapshot = result.toResponseSnapshot(objectMapper = objectMapper),
            )

            result
        } ?: error("Withdrawal transaction did not return a result")
    }

    private fun WalletErrorCode.httpStatus(): Int {
        return when (this) {
            WalletErrorCode.WALLET_NOT_FOUND -> HttpStatus.NOT_FOUND.value()
            WalletErrorCode.CURRENCY_MISMATCH,
            WalletErrorCode.INVALID_AMOUNT
            -> HttpStatus.BAD_REQUEST.value()
            WalletErrorCode.INSUFFICIENT_BALANCE,
            WalletErrorCode.IDEMPOTENCY_KEY_CONFLICT,
            WalletErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS,
            WalletErrorCode.WALLET_BUSY,
            WalletErrorCode.WALLET_CONCURRENT_MODIFICATION
            -> HttpStatus.CONFLICT.value()
        }
    }
}
