package com.wannabe.wallet.domain.wallet.exception

import com.wannabe.wallet.domain.wallet.error.WalletErrorCode

class WalletException(
    val code: WalletErrorCode,
    override val message: String = code.message,
) : RuntimeException(message)
