package com.wannabe.wallet.domain.wallet.service

import com.wannabe.wallet.domain.common.DomainService
import com.wannabe.wallet.domain.wallet.error.WalletErrorCode
import com.wannabe.wallet.domain.wallet.enums.OperationType
import com.wannabe.wallet.domain.wallet.enums.TransactionStatus
import com.wannabe.wallet.domain.wallet.enums.TransactionType
import com.wannabe.wallet.domain.wallet.exception.CurrencyMismatchException
import com.wannabe.wallet.domain.wallet.exception.InvalidAmountException
import com.wannabe.wallet.domain.wallet.exception.WalletNotFoundException
import com.wannabe.wallet.domain.wallet.model.IdempotencyRequest
import com.wannabe.wallet.domain.wallet.model.Wallet
import com.wannabe.wallet.domain.wallet.model.WalletTransaction
import com.wannabe.wallet.domain.wallet.model.WalletWithdrawalResult
import com.wannabe.wallet.domain.wallet.port.WalletCommandStore
import com.wannabe.wallet.domain.wallet.port.WalletQueryStore
import com.wannabe.wallet.domain.wallet.vo.Money
import com.wannabe.wallet.domain.wallet.vo.RequestHash
import java.time.LocalDateTime

@DomainService
class WalletDomainService(
    private val walletQueryStore: WalletQueryStore,
    private val walletCommandStore: WalletCommandStore,
) {
    fun validateWithdrawalAmount(money: Money) {
        if (!money.isPositive()) {
            throw InvalidAmountException()
        }
    }

    fun withdrawalRequestHash(
        walletId: String,
        money: Money,
        transactionId: String,
    ): String {
        return RequestHash.withdrawal(
            walletId = walletId,
            money = money,
            transactionId = transactionId,
        )
    }

    fun findIdempotencyRequest(walletId: String, transactionId: String): IdempotencyRequest? {
        return walletQueryStore.findIdempotencyRequest(
            walletId = walletId,
            idempotencyKey = transactionId,
        )
    }

    fun startWithdrawal(
        walletId: String,
        money: Money,
        transactionId: String,
        requestHash: String,
    ): Pair<Wallet, IdempotencyRequest> {
        val wallet = walletQueryStore.findWallet(walletId)
            ?: throw WalletNotFoundException()

        validateCurrency(
            wallet = wallet,
            money = money,
        )

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

        return wallet to idempotencyRequest
    }

    fun withdraw(
        wallet: Wallet,
        idempotencyRequest: IdempotencyRequest,
        money: Money,
        transactionId: String,
    ): WalletWithdrawalResult {
        val balanceBefore = wallet.balance
        val versionBefore = wallet.version

        if (balanceBefore < money) {
            return WalletWithdrawalResult.failure(
                transactionId = transactionId,
                wallet = wallet,
                money = money,
                balance = balanceBefore,
                walletVersion = versionBefore,
                failureCode = WalletErrorCode.INSUFFICIENT_BALANCE,
            )
        }

        val updatedRows = walletCommandStore.withdrawIfVersionMatches(
            walletId = wallet.walletId,
            version = versionBefore,
            money = money,
        )

        if (updatedRows != 1) {
            val latestWallet = walletQueryStore.findWallet(wallet.walletId)
                ?: throw WalletNotFoundException()
            val errorCode = if (latestWallet.balance < money) {
                WalletErrorCode.INSUFFICIENT_BALANCE
            } else {
                WalletErrorCode.WALLET_CONCURRENT_MODIFICATION
            }
            return WalletWithdrawalResult.failure(
                transactionId = transactionId,
                wallet = latestWallet,
                money = money,
                balance = latestWallet.balance,
                walletVersion = latestWallet.version,
                failureCode = errorCode,
            )
        }

        val transaction = walletCommandStore.saveTransaction(
            WalletTransaction(
                transactionId = transactionId,
                wallet = wallet,
                idempotencyRequest = idempotencyRequest,
                type = TransactionType.WITHDRAWAL,
                status = TransactionStatus.SUCCESS,
                money = money,
                balanceBefore = balanceBefore,
                balanceAfter = balanceBefore - money,
                walletVersionBefore = versionBefore,
                walletVersionAfter = versionBefore + 1,
            ),
        )

        return WalletWithdrawalResult.success(transaction = transaction)
    }

    fun completeIdempotencyRequest(
        idempotencyRequest: IdempotencyRequest,
        responseSnapshot: String,
    ) {
        idempotencyRequest.complete(
            responseSnapshot = responseSnapshot,
        )
        walletCommandStore.saveIdempotencyRequest(idempotencyRequest)
    }

    private fun validateCurrency(wallet: Wallet, money: Money) {
        if (wallet.currency != money.currency) {
            throw CurrencyMismatchException()
        }
    }
}
