package com.wannabe.wallet.domain.wallet.port

import com.wannabe.wallet.domain.wallet.model.IdempotencyRequest
import com.wannabe.wallet.domain.wallet.model.Wallet
import com.wannabe.wallet.domain.wallet.model.WalletTransaction
import java.math.BigDecimal

interface WalletCommandStore {
    fun findWallet(walletId: String): Wallet?

    fun findIdempotencyRequest(walletId: String, idempotencyKey: String): IdempotencyRequest?

    fun saveIdempotencyRequest(idempotencyRequest: IdempotencyRequest): IdempotencyRequest

    fun withdrawIfVersionMatches(
        walletId: String,
        version: Long,
        amount: BigDecimal,
        currency: String,
    ): Int

    fun saveTransaction(transaction: WalletTransaction): WalletTransaction
}
