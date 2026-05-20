package com.wannabe.wallet.infrastructure.adapter

import com.wannabe.wallet.domain.wallet.IdempotencyRequest
import com.wannabe.wallet.domain.wallet.Wallet
import com.wannabe.wallet.domain.wallet.WalletCommandStore
import com.wannabe.wallet.domain.wallet.WalletQueryStore
import com.wannabe.wallet.domain.wallet.WalletTransaction
import com.wannabe.wallet.infrastructure.jpa.IdempotencyRequestJpaRepository
import com.wannabe.wallet.infrastructure.jpa.WalletJpaRepository
import com.wannabe.wallet.infrastructure.jpa.WalletTransactionJpaRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class WalletPersistenceAdapter(
    private val walletJpaRepository: WalletJpaRepository,
    private val idempotencyRequestJpaRepository: IdempotencyRequestJpaRepository,
    private val walletTransactionJpaRepository: WalletTransactionJpaRepository,
) : WalletCommandStore, WalletQueryStore {
    override fun findWallet(walletId: String): Wallet? {
        return walletJpaRepository.findById(walletId).orElse(null)
    }

    override fun findIdempotencyRequest(walletId: String, idempotencyKey: String): IdempotencyRequest? {
        return idempotencyRequestJpaRepository
            .findByWalletWalletIdAndIdempotencyKey(walletId, idempotencyKey)
            .orElse(null)
    }

    override fun saveIdempotencyRequest(idempotencyRequest: IdempotencyRequest): IdempotencyRequest {
        return idempotencyRequestJpaRepository.saveAndFlush(idempotencyRequest)
    }

    override fun withdrawIfVersionMatches(
        walletId: String,
        version: Long,
        amount: BigDecimal,
        currency: String,
    ): Int {
        return walletJpaRepository.withdrawIfVersionMatches(walletId, version, amount, currency)
    }

    override fun saveTransaction(transaction: WalletTransaction): WalletTransaction {
        return walletTransactionJpaRepository.save(transaction)
    }

    override fun exists(walletId: String): Boolean {
        return walletJpaRepository.existsById(walletId)
    }

    override fun findTransactions(walletId: String): List<WalletTransaction> {
        return walletTransactionJpaRepository.findAllByWalletWalletIdOrderByProcessedAtDescIdDesc(walletId)
    }
}
