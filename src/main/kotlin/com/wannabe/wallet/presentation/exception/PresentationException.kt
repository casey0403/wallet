package com.wannabe.wallet.presentation.exception

import com.wannabe.wallet.presentation.error.PresentationErrorCode

class PresentationException(
    val code: PresentationErrorCode,
) : ApiCommonException(code)
