package com.wannabe.wallet.domain.wallet.model

import com.wannabe.wallet.domain.wallet.enums.TransactionStatus
import com.wannabe.wallet.domain.wallet.enums.TransactionType
import com.wannabe.wallet.domain.wallet.error.WalletErrorCode
import com.wannabe.wallet.domain.wallet.vo.Money
import java.time.LocalDateTime

class WalletWithdrawalResult(
    val transactionId: String,
    val walletId: String,
    val type: TransactionType,
    val status: TransactionStatus,
    val money: Money,
    val balance: Money,
    val walletVersion: Long,
    val processedAt: LocalDateTime,
    val failureCode: WalletErrorCode? = null,
) {
    companion object {
        fun success(transaction: WalletTransaction): WalletWithdrawalResult {
            return WalletWithdrawalResult(
                transactionId = transaction.transactionId,
                walletId = transaction.wallet.walletId,
                type = transaction.type,
                status = transaction.status,
                money = transaction.money,
                balance = transaction.balanceAfter,
                walletVersion = transaction.walletVersionAfter,
                processedAt = transaction.processedAt,
            )
        }

        fun failure(
            transactionId: String,
            wallet: Wallet,
            money: Money,
            balance: Money,
            walletVersion: Long,
            failureCode: WalletErrorCode,
        ): WalletWithdrawalResult {
            return WalletWithdrawalResult(
                transactionId = transactionId,
                walletId = wallet.walletId,
                type = TransactionType.WITHDRAWAL,
                status = TransactionStatus.FAILED,
                money = money,
                balance = balance,
                walletVersion = walletVersion,
                processedAt = LocalDateTime.now(),
                failureCode = failureCode,
            )
        }
    }
}
