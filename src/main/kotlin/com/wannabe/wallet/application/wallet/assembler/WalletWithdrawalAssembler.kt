package com.wannabe.wallet.application.wallet.assembler

import com.wannabe.wallet.application.wallet.dto.WithdrawalResultDTO

interface WalletWithdrawalAssembler {
    fun withdraw(
        walletId: String,
        amount: Long,
        currency: String,
        transactionId: String,
    ): WithdrawalResultDTO
}
