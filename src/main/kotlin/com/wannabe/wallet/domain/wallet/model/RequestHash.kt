package com.wannabe.wallet.domain.wallet.model

import java.security.MessageDigest

object RequestHash {
    fun withdrawal(walletId: String, money: Money, transactionId: String): String {
        val normalized = listOf(
            walletId,
            "WITHDRAWAL",
            money.amount.stripTrailingZeros().toPlainString(),
            money.currency,
            transactionId,
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
