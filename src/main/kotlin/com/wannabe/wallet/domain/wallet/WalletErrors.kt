package com.wannabe.wallet.domain.wallet

import org.springframework.http.HttpStatus

enum class WalletErrorCode(
    val status: HttpStatus,
    val message: String,
) {
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "Wallet not found"),
    INSUFFICIENT_BALANCE(HttpStatus.CONFLICT, "Insufficient wallet balance"),
    IDEMPOTENCY_KEY_CONFLICT(HttpStatus.CONFLICT, "Transaction id was reused with different request parameters"),
    IDEMPOTENCY_REQUEST_IN_PROGRESS(HttpStatus.CONFLICT, "Same idempotency request is still processing"),
    WALLET_BUSY(HttpStatus.CONFLICT, "Wallet is busy. Please retry"),
    WALLET_CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "Wallet balance was changed by another transaction. Please retry"),
    CURRENCY_MISMATCH(HttpStatus.BAD_REQUEST, "Requested currency does not match wallet currency"),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "Amount must be greater than zero"),
}

class WalletException(
    val code: WalletErrorCode,
    override val message: String = code.message,
) : RuntimeException(message)
