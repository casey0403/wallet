package com.wannabe.wallet.domain.wallet.error

enum class WalletErrorCode(
    val message: String,
) {
    WALLET_NOT_FOUND("Wallet not found"),
    INSUFFICIENT_BALANCE("Insufficient wallet balance"),
    IDEMPOTENCY_KEY_CONFLICT("Transaction id was reused with different request parameters"),
    IDEMPOTENCY_REQUEST_IN_PROGRESS("Same idempotency request is still processing"),
    WALLET_BUSY("Wallet is busy. Please retry"),
    WALLET_CONCURRENT_MODIFICATION("Wallet balance was changed by another transaction. Please retry"),
    CURRENCY_MISMATCH("Requested currency does not match wallet currency"),
    INVALID_AMOUNT("Amount must be greater than zero"),
}
