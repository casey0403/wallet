package com.wannabe.wallet.application.wallet.assembler

import com.wannabe.wallet.application.wallet.dto.TransactionDTO
import com.wannabe.wallet.domain.wallet.model.WalletTransaction
import org.springframework.stereotype.Component

@Component
class WalletAssembler {
    fun toResult(transaction: WalletTransaction): TransactionDTO {
        return TransactionDTO(
            transactionId = transaction.transactionId,
            walletId = transaction.wallet.walletId,
            type = transaction.type.name,
            status = transaction.status.name,
            withdrawalAmount = transaction.money.amount,
            currency = transaction.money.currency,
            balance = transaction.balanceAfter.amount,
            version = transaction.walletVersionAfter,
            withdrawalDate = transaction.processedAt,
        )
    }
}
