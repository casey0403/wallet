package com.wannabe.wallet.infrastructure.jpa

import com.wannabe.wallet.domain.wallet.Wallet
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal

interface WalletJpaRepository : JpaRepository<Wallet, String> {
    @Modifying(flushAutomatically = true)
    @Query(
        value = """
            UPDATE wallets
               SET balance = balance - :amount,
                   version = version + 1
             WHERE id = :walletId
               AND version = :version
               AND balance >= :amount
               AND currency = :currency
               AND wallet_status = 'ACTIVE'
        """,
        nativeQuery = true,
    )
    fun withdrawIfVersionMatches(
        @Param("walletId") walletId: String,
        @Param("version") version: Long,
        @Param("amount") amount: BigDecimal,
        @Param("currency") currency: String,
    ): Int
}
