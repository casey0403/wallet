package com.wannabe.wallet.domain.wallet

import java.math.BigDecimal
import java.security.MessageDigest

object RequestHash {
    fun withdrawal(walletId: String, amount: BigDecimal, currency: String, transactionId: String): String {
        val normalized = listOf(
            walletId,
            "WITHDRAWAL",
            amount.stripTrailingZeros().toPlainString(),
            currency,
            transactionId,
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
