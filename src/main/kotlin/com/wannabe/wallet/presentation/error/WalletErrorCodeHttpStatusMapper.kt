package com.wannabe.wallet.presentation.error

import com.wannabe.wallet.domain.wallet.error.WalletErrorCode
import org.springframework.http.HttpStatus

fun WalletErrorCode.toHttpStatus(): HttpStatus {
    return when (this) {
        WalletErrorCode.WALLET_NOT_FOUND -> HttpStatus.NOT_FOUND
        WalletErrorCode.CURRENCY_MISMATCH,
        WalletErrorCode.INVALID_AMOUNT,
        -> HttpStatus.BAD_REQUEST
        WalletErrorCode.INSUFFICIENT_BALANCE,
        WalletErrorCode.IDEMPOTENCY_KEY_CONFLICT,
        WalletErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS,
        WalletErrorCode.WALLET_BUSY,
        WalletErrorCode.WALLET_CONCURRENT_MODIFICATION,
        -> HttpStatus.CONFLICT
    }
}
