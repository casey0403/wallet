package com.wannabe.wallet.application.wallet.assembler

import com.wannabe.wallet.application.wallet.dto.TransactionDTO
import com.wannabe.wallet.domain.wallet.enums.TransactionType

interface WalletTransactionHistoryAssembler {
    fun getTransactions(walletId: String, transactionType: TransactionType? = null): List<TransactionDTO>
}
