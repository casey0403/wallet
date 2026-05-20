package com.wannabe.wallet.domain.wallet.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "wallets")
class Wallet(
    @Id
    @Column(name = "id", nullable = false, length = 64)
    val walletId: String,

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    var balance: BigDecimal,

    @Column(name = "user_id", length = 64)
    val userId: String? = null,

    @Column(name = "currency", nullable = false, length = 3)
    val currency: String = "KRW",

    @Column(name = "version", nullable = false)
    var version: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_status", nullable = false, length = 20)
    val walletStatus: WalletStatus = WalletStatus.ACTIVE,

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    val createdAt: LocalDateTime? = null,

    @Column(name = "updated_at", nullable = false, insertable = false)
    val updatedAt: LocalDateTime? = null,
)

enum class WalletStatus {
    ACTIVE,
    SUSPENDED,
}
