package com.wannabe.wallet.domain.wallet.vo

import com.wannabe.wallet.application.wallet.exception.CurrencyMismatchException
import java.math.BigDecimal

data class Money(
    val amount: BigDecimal,
    val currency: String,
) : Comparable<Money> {
    init {
        if (!ISO_CURRENCY_PATTERN.matches(currency)) {
            throw CurrencyMismatchException()
        }
    }

    override fun compareTo(other: Money): Int {
        validateSameCurrency(other)
        return amount.compareTo(other.amount)
    }

    operator fun minus(other: Money): Money {
        validateSameCurrency(other)
        return copy(amount = amount - other.amount)
    }

    fun isPositive(): Boolean {
        return amount > BigDecimal.ZERO
    }

    private fun validateSameCurrency(other: Money) {
        if (currency != other.currency) {
            throw CurrencyMismatchException()
        }
    }

    companion object {
        private val ISO_CURRENCY_PATTERN = Regex("^[A-Z]{3}$")
    }
}
