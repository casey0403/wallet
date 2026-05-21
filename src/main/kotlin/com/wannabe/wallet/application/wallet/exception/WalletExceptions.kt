package com.wannabe.wallet.application.wallet.exception

import com.wannabe.wallet.application.wallet.error.WalletErrorCode
import com.wannabe.wallet.presentation.exception.ApiCommonException

abstract class AbstractWalletException(val code: WalletErrorCode, vararg args: Any) :
    ApiCommonException(code, args)

class WalletNotFoundException : AbstractWalletException(WalletErrorCode.WALLET_NOT_FOUND)

class InsufficientBalanceException : AbstractWalletException(WalletErrorCode.INSUFFICIENT_BALANCE)

class IdempotencyKeyConflictException : AbstractWalletException(WalletErrorCode.IDEMPOTENCY_KEY_CONFLICT)

class IdempotencyRequestInProgressException : AbstractWalletException(WalletErrorCode.IDEMPOTENCY_REQUEST_IN_PROGRESS)

class WalletBusyException : AbstractWalletException(WalletErrorCode.WALLET_BUSY)

class WalletConcurrentModificationException : AbstractWalletException(WalletErrorCode.WALLET_CONCURRENT_MODIFICATION)

class CurrencyMismatchException : AbstractWalletException(WalletErrorCode.CURRENCY_MISMATCH)

class InvalidAmountException : AbstractWalletException(WalletErrorCode.INVALID_AMOUNT)
