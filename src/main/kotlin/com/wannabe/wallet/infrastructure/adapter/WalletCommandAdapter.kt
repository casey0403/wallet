package com.wannabe.wallet.infrastructure.adapter

import com.wannabe.wallet.domain.wallet.model.IdempotencyRequest
import com.wannabe.wallet.domain.wallet.model.WalletTransaction
import com.wannabe.wallet.domain.wallet.port.WalletCommandStore
import com.wannabe.wallet.domain.wallet.vo.Money
import com.wannabe.wallet.infrastructure.jpa.IdempotencyRequestJpaRepository
import com.wannabe.wallet.infrastructure.jpa.WalletJpaRepository
import com.wannabe.wallet.infrastructure.jpa.WalletTransactionJpaRepository
import com.wannabe.wallet.infrastructure.jpa.entity.toJPAEntity
import org.springframework.stereotype.Component

@Component
class WalletCommandAdapter(
    private val walletJpaRepository: WalletJpaRepository,
    private val idempotencyRequestJpaRepository: IdempotencyRequestJpaRepository,
    private val walletTransactionJpaRepository: WalletTransactionJpaRepository,
) : WalletCommandStore {
    override fun saveIdempotencyRequest(idempotencyRequest: IdempotencyRequest): IdempotencyRequest {
        val walletJPAEntity = walletJpaRepository.getReferenceById(idempotencyRequest.wallet.walletId)
        return idempotencyRequestJpaRepository.saveAndFlush(
            idempotencyRequest.toJPAEntity(walletJPAEntity = walletJPAEntity),
        ).toDomain()
    }

    override fun withdrawIfVersionMatches(
        walletId: String,
        version: Long,
        money: Money,
    ): Int {
        return walletJpaRepository.withdrawIfVersionMatches(
            walletId = walletId,
            version = version,
            amount = money.amount,
            currency = money.currency,
        )
    }

    override fun saveTransaction(transaction: WalletTransaction): WalletTransaction {
        val walletJPAEntity = walletJpaRepository.getReferenceById(transaction.wallet.walletId)
        val idempotencyRequestJPAEntity = idempotencyRequestJpaRepository.getReferenceById(transaction.idempotencyRequest.id)
        return walletTransactionJpaRepository.save(
            transaction.toJPAEntity(
                walletJPAEntity = walletJPAEntity,
                idempotencyRequestJPAEntity = idempotencyRequestJPAEntity,
            ),
        ).toDomain()
    }
}
