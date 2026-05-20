package com.wannabe.wallet.infrastructure.jpa

import com.wannabe.wallet.infrastructure.jpa.entity.WalletTransactionJPAEntity
import org.springframework.data.jpa.repository.JpaRepository

interface WalletTransactionJpaRepository : JpaRepository<WalletTransactionJPAEntity, Long> {
    fun findAllByWalletWalletIdOrderByProcessedAtDescIdDesc(walletId: String): List<WalletTransactionJPAEntity>
}
