package com.wannabe.wallet.presentation.exception

import com.wannabe.wallet.presentation.error.ErrorCode

abstract class ApiCommonException(
    val errorCode: ErrorCode,
    val args: Array<out Any> = emptyArray(),
) : RuntimeException() {
    override val message: String = errorCode.message
}
