package com.wannabe.wallet.application.wallet.assembler

import com.wannabe.wallet.application.wallet.dto.TransactionDTO

interface WalletTransactionHistoryAssembler {
    fun getTransactions(walletId: String): List<TransactionDTO>
}
