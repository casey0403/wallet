package com.wannabe.wallet.domain.wallet.port

object WalletLockKey {
    private const val WALLET_WITHDRAW_PREFIX = "wallet:withdraw"

    fun withdraw(walletId: String): String {
        return "$WALLET_WITHDRAW_PREFIX:$walletId"
    }
}
