package com.wannabe.wallet.domain.wallet.port

interface WalletLock {
    fun <T> execute(walletId: String, block: () -> T): T
}
