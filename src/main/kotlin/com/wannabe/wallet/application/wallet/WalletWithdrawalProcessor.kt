package com.wannabe.wallet.application.wallet

import com.fasterxml.jackson.databind.ObjectMapper
import com.wannabe.wallet.application.wallet.dto.TransactionResult
import com.wannabe.wallet.application.wallet.dto.WithdrawalResult
import com.wannabe.wallet.domain.wallet.IdempotencyRequest
import com.wannabe.wallet.domain.wallet.IdempotencyStatus
import com.wannabe.wallet.domain.wallet.OperationType
import com.wannabe.wallet.domain.wallet.TransactionStatus
import com.wannabe.wallet.domain.wallet.TransactionType
import com.wannabe.wallet.domain.wallet.Wallet
import com.wannabe.wallet.domain.wallet.WalletCommandStore
import com.wannabe.wallet.domain.wallet.WalletDomainService
import com.wannabe.wallet.domain.wallet.WalletErrorCode
import com.wannabe.wallet.domain.wallet.WalletException
import com.wannabe.wallet.domain.wallet.WalletTransaction
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class WalletWithdrawalProcessor(
    private val walletCommandStore: WalletCommandStore,
    private val walletDomainService: WalletDomainService,
    private val walletAssembler: WalletAssembler,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun withdraw(walletId: String, amount: BigDecimal, currency: String, transactionId: String): WithdrawalResult {
        val wallet = walletCommandStore.findWallet(walletId)
            ?: throw WalletException(WalletErrorCode.WALLET_NOT_FOUND)

        walletDomainService.validateCurrency(wallet, currency)

        val requestHash = walletDomainService.withdrawalRequestHash(walletId, amount, currency, transactionId)
        val existing = walletCommandStore.findIdempotencyRequest(walletId, transactionId)

        if (existing != null) {
            return existing.toResult(requestHash)
        }

        val idempotencyRequest = walletCommandStore.saveIdempotencyRequest(
            IdempotencyRequest(
                wallet = wallet,
                idempotencyKey = transactionId,
                requestHash = requestHash,
                operationType = OperationType.WITHDRAWAL,
                amount = amount,
                currency = currency,
                expiresAt = LocalDateTime.now().plusDays(7),
            ),
        )

        val result = executeFirstWithdrawal(wallet, idempotencyRequest, amount, currency, transactionId)
        idempotencyRequest.complete(result.httpStatus, objectMapper.writeValueAsString(result.body))
        return result
    }

    private fun IdempotencyRequest.toResult(requestHash: String): WithdrawalResult {
        if (this.requestHash != requestHash) {
            throw WalletException(WalletErrorCode.IDEMPOTENCY_KEY_CONFLICT)
        }

        if (status != IdempotencyStatus.COMPLETED || httpStatus == null || responseSnapshot == null) {
            throw WalletException(WalletErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS)
        }

        return WithdrawalResult(
            httpStatus = httpStatus!!,
            body = objectMapper.readValue(responseSnapshot, TransactionResult::class.java),
        )
    }

    private fun executeFirstWithdrawal(
        wallet: Wallet,
        idempotencyRequest: IdempotencyRequest,
        amount: BigDecimal,
        currency: String,
        transactionId: String,
    ): WithdrawalResult {
        val balanceBefore = wallet.balance
        val versionBefore = wallet.version

        if (balanceBefore < amount) {
            return recordFailure(
                wallet = wallet,
                idempotencyRequest = idempotencyRequest,
                amount = amount,
                currency = currency,
                transactionId = transactionId,
                balance = balanceBefore,
                version = versionBefore,
                errorCode = WalletErrorCode.INSUFFICIENT_BALANCE,
            )
        }

        val updatedRows = walletCommandStore.withdrawIfVersionMatches(
            walletId = wallet.walletId,
            version = versionBefore,
            amount = amount,
            currency = currency,
        )

        if (updatedRows != 1) {
            val latestWallet = walletCommandStore.findWallet(wallet.walletId)
                ?: throw WalletException(WalletErrorCode.WALLET_NOT_FOUND)
            val errorCode = if (latestWallet.balance < amount) {
                WalletErrorCode.INSUFFICIENT_BALANCE
            } else {
                WalletErrorCode.WALLET_CONCURRENT_MODIFICATION
            }
            return recordFailure(
                wallet = latestWallet,
                idempotencyRequest = idempotencyRequest,
                amount = amount,
                currency = currency,
                transactionId = transactionId,
                balance = latestWallet.balance,
                version = latestWallet.version,
                errorCode = errorCode,
            )
        }

        val balanceAfter = balanceBefore - amount
        val versionAfter = versionBefore + 1
        val transaction = walletCommandStore.saveTransaction(
            WalletTransaction(
                transactionId = transactionId,
                wallet = wallet,
                idempotencyRequest = idempotencyRequest,
                type = TransactionType.WITHDRAWAL,
                status = TransactionStatus.SUCCESS,
                amount = amount,
                currency = currency,
                balanceBefore = balanceBefore,
                balanceAfter = balanceAfter,
                walletVersionBefore = versionBefore,
                walletVersionAfter = versionAfter,
            ),
        )

        return WithdrawalResult(HttpStatus.OK.value(), walletAssembler.toResult(transaction))
    }

    private fun recordFailure(
        wallet: Wallet,
        idempotencyRequest: IdempotencyRequest,
        amount: BigDecimal,
        currency: String,
        transactionId: String,
        balance: BigDecimal,
        version: Long,
        errorCode: WalletErrorCode,
    ): WithdrawalResult {
        val transaction = walletCommandStore.saveTransaction(
            WalletTransaction(
                transactionId = transactionId,
                wallet = wallet,
                idempotencyRequest = idempotencyRequest,
                type = TransactionType.WITHDRAWAL,
                status = TransactionStatus.FAILED,
                amount = amount,
                currency = currency,
                balanceBefore = balance,
                balanceAfter = balance,
                walletVersionBefore = version,
                walletVersionAfter = version,
                failureCode = errorCode.name,
                failureMessage = errorCode.message,
            ),
        )
        return WithdrawalResult(errorCode.status.value(), walletAssembler.toResult(transaction))
    }
}
