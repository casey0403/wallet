package com.wannabe.wallet.infrastructure.jpa

import com.wannabe.wallet.domain.wallet.model.WalletTransaction
import org.springframework.data.jpa.repository.JpaRepository

interface WalletTransactionJpaRepository : JpaRepository<WalletTransaction, Long> {
    fun findAllByWalletWalletIdOrderByProcessedAtDescIdDesc(walletId: String): List<WalletTransaction>
}
