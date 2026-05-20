package com.wannabe.wallet.domain.wallet

import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
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
