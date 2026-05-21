package com.wannabe.wallet.domain.wallet.port

import com.wannabe.wallet.domain.wallet.model.IdempotencyRequest
import com.wannabe.wallet.domain.wallet.model.Wallet
import com.wannabe.wallet.domain.wallet.model.WalletTransaction

interface WalletQueryStore {
    fun exists(walletId: String): Boolean

    fun findWallet(walletId: String): Wallet?

    fun findIdempotencyRequest(walletId: String, idempotencyKey: String): IdempotencyRequest?

    fun findTransactions(walletId: String): List<WalletTransaction>
}
