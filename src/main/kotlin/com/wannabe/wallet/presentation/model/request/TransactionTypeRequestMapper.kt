package com.wannabe.wallet.presentation.model.request

import com.wannabe.wallet.domain.wallet.enums.TransactionType
import com.wannabe.wallet.presentation.error.PresentationErrorCode
import com.wannabe.wallet.presentation.exception.PresentationException

fun String?.toTransactionTypeFilter(): TransactionType? {
    if (this == null) {
        return null
    }

    val enumName = when (val normalized = trim().uppercase()) {
        "WITHDRAW" -> "WITHDRAWAL"
        else -> normalized
    }

    return TransactionType.entries.find { it.name == enumName }
        ?: throw PresentationException(PresentationErrorCode.INVALID_TRANSACTION_TYPE)
}
