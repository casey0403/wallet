package com.wannabe.wallet.domain.wallet.port

import com.wannabe.wallet.domain.wallet.model.WalletTransaction

interface WalletQueryStore {
    fun exists(walletId: String): Boolean

    fun findTransactions(walletId: String): List<WalletTransaction>
}
