package com.wannabe.wallet.domain.wallet.port

import com.wannabe.wallet.domain.wallet.model.IdempotencyRequest
import com.wannabe.wallet.domain.wallet.vo.Money
import com.wannabe.wallet.domain.wallet.model.WalletTransaction

interface WalletCommandStore {
    fun saveIdempotencyRequest(idempotencyRequest: IdempotencyRequest): IdempotencyRequest

    fun withdrawIfVersionMatches(
        walletId: String,
        version: Long,
        money: Money,
    ): Int

    fun saveTransaction(transaction: WalletTransaction): WalletTransaction
}
