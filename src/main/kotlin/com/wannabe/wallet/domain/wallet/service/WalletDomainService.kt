package com.wannabe.wallet.domain.wallet.service

import com.wannabe.wallet.domain.wallet.exception.WalletErrorCode
import com.wannabe.wallet.domain.wallet.exception.WalletException
import com.wannabe.wallet.domain.wallet.model.RequestHash
import com.wannabe.wallet.domain.wallet.model.Wallet
import java.math.BigDecimal

class WalletDomainService {
    fun validateWithdrawalAmount(amount: BigDecimal) {
        if (amount <= BigDecimal.ZERO) {
            throw WalletException(WalletErrorCode.INVALID_AMOUNT)
        }
    }

    fun validateCurrency(wallet: Wallet, currency: String) {
        if (wallet.currency != currency) {
            throw WalletException(WalletErrorCode.CURRENCY_MISMATCH)
        }
    }

    fun withdrawalRequestHash(
        walletId: String,
        amount: BigDecimal,
        currency: String,
        transactionId: String,
    ): String {
        return RequestHash.withdrawal(walletId, amount, currency, transactionId)
    }
}
