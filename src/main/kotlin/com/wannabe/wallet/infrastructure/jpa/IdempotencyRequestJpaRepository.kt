package com.wannabe.wallet.infrastructure.jpa

import com.wannabe.wallet.infrastructure.jpa.entity.IdempotencyRequestJPAEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface IdempotencyRequestJpaRepository : JpaRepository<IdempotencyRequestJPAEntity, Long> {
    fun findByWalletWalletIdAndIdempotencyKey(walletId: String, idempotencyKey: String): Optional<IdempotencyRequestJPAEntity>
}
