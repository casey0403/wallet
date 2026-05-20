package com.wannabe.wallet.domain.wallet.model

import com.wannabe.wallet.domain.wallet.enums.WalletStatus
import com.wannabe.wallet.domain.wallet.vo.Money

class Wallet(
    val walletId: String,
    val balance: Money,
    val userId: String? = null,
    val version: Long = 0,
    val walletStatus: WalletStatus = WalletStatus.ACTIVE,
) {
    val currency: String
        get() = balance.currency
}
