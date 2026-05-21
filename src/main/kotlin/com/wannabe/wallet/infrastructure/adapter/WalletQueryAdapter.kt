package com.wannabe.wallet.infrastructure.adapter

import com.wannabe.wallet.domain.wallet.model.IdempotencyRequest
import com.wannabe.wallet.domain.wallet.model.Wallet
import com.wannabe.wallet.domain.wallet.model.WalletTransaction
import com.wannabe.wallet.domain.wallet.port.WalletQueryStore
import com.wannabe.wallet.infrastructure.jpa.IdempotencyRequestJpaRepository
import com.wannabe.wallet.infrastructure.jpa.WalletJpaRepository
import com.wannabe.wallet.infrastructure.jpa.WalletTransactionJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class WalletQueryAdapter(
    private val walletJpaRepository: WalletJpaRepository,
    private val idempotencyRequestJpaRepository: IdempotencyRequestJpaRepository,
    private val walletTransactionJpaRepository: WalletTransactionJpaRepository,
) : WalletQueryStore {
    override fun exists(walletId: String): Boolean {
        return walletJpaRepository.existsById(walletId)
    }

    @Transactional(readOnly = true)
    override fun findWallet(walletId: String): Wallet? {
        return walletJpaRepository.findById(walletId).map { it.toDomain() }.orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findIdempotencyRequest(walletId: String, idempotencyKey: String): IdempotencyRequest? {
        return idempotencyRequestJpaRepository
            .findByWalletWalletIdAndIdempotencyKey(
                walletId = walletId,
                idempotencyKey = idempotencyKey,
            )
            .map { it.toDomain() }
            .orElse(null)
    }

    override fun findTransactions(walletId: String): List<WalletTransaction> {
        return walletTransactionJpaRepository.findAllByWalletWalletIdOrderByProcessedAtDescIdDesc(walletId)
            .map { it.toDomain() }
    }
}
