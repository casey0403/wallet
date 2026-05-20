package com.wannabe.wallet.application.wallet

import com.wannabe.wallet.application.wallet.dto.TransactionResult
import com.wannabe.wallet.domain.wallet.WalletTransaction
import org.springframework.stereotype.Component

@Component
class WalletAssembler {
    fun toResult(transaction: WalletTransaction): TransactionResult {
        return TransactionResult(
            transactionId = transaction.transactionId,
            walletId = transaction.wallet.walletId,
            type = transaction.type.name,
            status = transaction.status.name,
            withdrawalAmount = transaction.amount,
            currency = transaction.currency,
            balance = transaction.balanceAfter,
            version = transaction.walletVersionAfter,
            withdrawalDate = transaction.processedAt,
            failureCode = transaction.failureCode,
            failureMessage = transaction.failureMessage,
        )
    }
}
