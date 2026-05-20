package com.wannabe.wallet.domain.wallet

interface WalletQueryStore {
    fun exists(walletId: String): Boolean

    fun findTransactions(walletId: String): List<WalletTransaction>
}
