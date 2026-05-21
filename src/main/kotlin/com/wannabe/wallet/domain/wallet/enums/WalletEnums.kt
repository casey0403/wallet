package com.wannabe.wallet.domain.wallet.enums

enum class WalletStatus {
    ACTIVE,
    SUSPENDED,
}

enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
}

enum class TransactionStatus {
    SUCCESS,
    FAILED,
}

enum class OperationType {
    DEPOSIT,
    WITHDRAWAL,
}

enum class IdempotencyStatus {
    PROCESSING,
    COMPLETED,
}
