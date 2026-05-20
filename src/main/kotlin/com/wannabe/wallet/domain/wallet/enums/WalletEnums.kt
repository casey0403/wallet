package com.wannabe.wallet.domain.wallet.enums

enum class WalletStatus {
    ACTIVE,
    SUSPENDED,
}

enum class TransactionType {
    WITHDRAWAL,
}

enum class TransactionStatus {
    SUCCESS,
    FAILED,
}

enum class OperationType {
    WITHDRAWAL,
}

enum class IdempotencyStatus {
    PROCESSING,
    COMPLETED,
}
