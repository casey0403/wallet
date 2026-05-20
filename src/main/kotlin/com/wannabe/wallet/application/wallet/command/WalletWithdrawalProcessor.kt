package com.wannabe.wallet.application.wallet.command

import com.fasterxml.jackson.databind.ObjectMapper
import com.wannabe.wallet.application.wallet.dto.TransactionDTO
import com.wannabe.wallet.application.wallet.dto.WithdrawalResultDTO
import com.wannabe.wallet.application.wallet.assembler.WalletAssembler
import com.wannabe.wallet.domain.wallet.model.IdempotencyRequest
import com.wannabe.wallet.domain.wallet.enums.IdempotencyStatus
import com.wannabe.wallet.domain.wallet.vo.Money
import com.wannabe.wallet.domain.wallet.enums.OperationType
import com.wannabe.wallet.domain.wallet.enums.TransactionStatus
import com.wannabe.wallet.domain.wallet.enums.TransactionType
import com.wannabe.wallet.domain.wallet.model.Wallet
import com.wannabe.wallet.domain.wallet.port.WalletCommandStore
import com.wannabe.wallet.domain.wallet.service.WalletDomainService
import com.wannabe.wallet.domain.wallet.exception.WalletErrorCode
import com.wannabe.wallet.domain.wallet.exception.WalletException
import com.wannabe.wallet.domain.wallet.model.WalletTransaction
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class WalletWithdrawalProcessor(
    private val walletCommandStore: WalletCommandStore,
    private val walletDomainService: WalletDomainService,
    private val walletAssembler: WalletAssembler,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun withdraw(walletId: String, money: Money, transactionId: String): WithdrawalResultDTO {
        val wallet = walletCommandStore.findWallet(walletId)
            ?: throw WalletException(WalletErrorCode.WALLET_NOT_FOUND)

        walletDomainService.validateCurrency(
            wallet = wallet,
            money = money,
        )

        val requestHash = walletDomainService.withdrawalRequestHash(
            walletId = walletId,
            money = money,
            transactionId = transactionId,
        )
        val existing = walletCommandStore.findIdempotencyRequest(
            walletId = walletId,
            idempotencyKey = transactionId,
        )

        if (existing != null) {
            return existing.toResult(requestHash)
        }

        val idempotencyRequest = walletCommandStore.saveIdempotencyRequest(
            IdempotencyRequest(
                wallet = wallet,
                idempotencyKey = transactionId,
                requestHash = requestHash,
                operationType = OperationType.WITHDRAWAL,
                money = money,
                expiresAt = LocalDateTime.now().plusDays(7),
            ),
        )

        val result = executeFirstWithdrawal(
            wallet = wallet,
            idempotencyRequest = idempotencyRequest,
            money = money,
            transactionId = transactionId,
        )
        idempotencyRequest.complete(
            httpStatus = result.httpStatus,
            responseSnapshot = objectMapper.writeValueAsString(result.body),
        )
        walletCommandStore.saveIdempotencyRequest(idempotencyRequest)
        return result
    }

    private fun IdempotencyRequest.toResult(requestHash: String): WithdrawalResultDTO {
        if (this.requestHash != requestHash) {
            throw WalletException(WalletErrorCode.IDEMPOTENCY_KEY_CONFLICT)
        }

        if (status != IdempotencyStatus.COMPLETED || httpStatus == null || responseSnapshot == null) {
            throw WalletException(WalletErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS)
        }

        return WithdrawalResultDTO(
            httpStatus = httpStatus!!,
            body = objectMapper.readValue(responseSnapshot, TransactionDTO::class.java),
        )
    }

    private fun executeFirstWithdrawal(
        wallet: Wallet,
        idempotencyRequest: IdempotencyRequest,
        money: Money,
        transactionId: String,
    ): WithdrawalResultDTO {
        val balanceBefore = wallet.balance
        val versionBefore = wallet.version

        if (balanceBefore < money) {
            return recordFailure(
                wallet = wallet,
                idempotencyRequest = idempotencyRequest,
                money = money,
                transactionId = transactionId,
                balance = balanceBefore,
                version = versionBefore,
                errorCode = WalletErrorCode.INSUFFICIENT_BALANCE,
            )
        }

        val updatedRows = walletCommandStore.withdrawIfVersionMatches(
            walletId = wallet.walletId,
            version = versionBefore,
            money = money,
        )

        if (updatedRows != 1) {
            val latestWallet = walletCommandStore.findWallet(wallet.walletId)
                ?: throw WalletException(WalletErrorCode.WALLET_NOT_FOUND)
            val errorCode = if (latestWallet.balance < money) {
                WalletErrorCode.INSUFFICIENT_BALANCE
            } else {
                WalletErrorCode.WALLET_CONCURRENT_MODIFICATION
            }
            return recordFailure(
                wallet = latestWallet,
                idempotencyRequest = idempotencyRequest,
                money = money,
                transactionId = transactionId,
                balance = latestWallet.balance,
                version = latestWallet.version,
                errorCode = errorCode,
            )
        }

        val balanceAfter = balanceBefore - money
        val versionAfter = versionBefore + 1
        val transaction = walletCommandStore.saveTransaction(
            WalletTransaction(
                transactionId = transactionId,
                wallet = wallet,
                idempotencyRequest = idempotencyRequest,
                type = TransactionType.WITHDRAWAL,
                status = TransactionStatus.SUCCESS,
                money = money,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                walletVersionBefore = versionBefore,
                walletVersionAfter = versionAfter,
            ),
        )

        return WithdrawalResultDTO(
            httpStatus = HttpStatus.OK.value(),
            body = walletAssembler.toResult(transaction),
        )
    }

    private fun recordFailure(
        wallet: Wallet,
        idempotencyRequest: IdempotencyRequest,
        money: Money,
        transactionId: String,
        balance: Money,
        version: Long,
        errorCode: WalletErrorCode,
    ): WithdrawalResultDTO {
        val transaction = walletCommandStore.saveTransaction(
            WalletTransaction(
                transactionId = transactionId,
                wallet = wallet,
                idempotencyRequest = idempotencyRequest,
                type = TransactionType.WITHDRAWAL,
                status = TransactionStatus.FAILED,
                money = money,
                balanceBefore = balance,
                balanceAfter = balance,
                walletVersionBefore = version,
                walletVersionAfter = version,
                failureCode = errorCode.name,
                failureMessage = errorCode.message,
            ),
        )
        return WithdrawalResultDTO(
            httpStatus = errorCode.httpStatus(),
            body = walletAssembler.toResult(transaction),
        )
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
