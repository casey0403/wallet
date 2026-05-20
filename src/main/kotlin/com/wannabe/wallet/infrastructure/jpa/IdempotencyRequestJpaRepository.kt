package com.wannabe.wallet.infrastructure.jpa

import com.wannabe.wallet.domain.wallet.IdempotencyRequest
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface IdempotencyRequestJpaRepository : JpaRepository<IdempotencyRequest, Long> {
    fun findByWalletWalletIdAndIdempotencyKey(walletId: String, idempotencyKey: String): Optional<IdempotencyRequest>
}
