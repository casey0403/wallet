package com.wannabe.wallet.application.wallet.assembler

import com.wannabe.wallet.application.wallet.dto.WithdrawalResultDTO
import java.math.BigDecimal

interface WalletWithdrawalAssembler {
    fun withdraw(
        walletId: String,
        amount: BigDecimal,
        currency: String,
        transactionId: String,
    ): WithdrawalResultDTO
}
