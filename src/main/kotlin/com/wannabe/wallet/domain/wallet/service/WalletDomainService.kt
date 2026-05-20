package com.wannabe.wallet.domain.wallet.service

import com.wannabe.wallet.domain.wallet.error.WalletErrorCode
import com.wannabe.wallet.domain.wallet.exception.WalletException
import com.wannabe.wallet.domain.wallet.vo.Money
import com.wannabe.wallet.domain.wallet.vo.RequestHash
import com.wannabe.wallet.domain.wallet.model.Wallet

class WalletDomainService {
    fun validateWithdrawalAmount(money: Money) {
        if (!money.isPositive()) {
            throw WalletException(WalletErrorCode.INVALID_AMOUNT)
        }
    }

    fun validateCurrency(wallet: Wallet, money: Money) {
        if (wallet.currency != money.currency) {
            throw WalletException(WalletErrorCode.CURRENCY_MISMATCH)
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
}
