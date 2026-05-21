package com.wannabe.wallet.domain.wallet.port

interface WalletLock {
    fun <T> execute(key: String, block: () -> T): T
}
