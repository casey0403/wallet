package com.wannabe.wallet.application.wallet.assembler

import com.wannabe.wallet.application.common.ApplicationService
import com.wannabe.wallet.application.wallet.dto.TransactionDTO
import com.wannabe.wallet.application.wallet.dto.toTransactionDTO
import com.wannabe.wallet.domain.wallet.enums.TransactionType
import com.wannabe.wallet.domain.wallet.exception.WalletNotFoundException
import com.wannabe.wallet.domain.wallet.port.WalletQueryStore
import org.springframework.transaction.annotation.Transactional

@ApplicationService
class WalletTransactionHistoryAssemblerImpl(
    private val walletQueryStore: WalletQueryStore,
) : WalletTransactionHistoryAssembler {
    @Transactional(readOnly = true)
    override fun getTransactions(walletId: String, transactionType: TransactionType?): List<TransactionDTO> {
        if (!walletQueryStore.exists(walletId)) {
            throw WalletNotFoundException()
        }

        return walletQueryStore.findTransactions(walletId, transactionType)
            .map { it.toTransactionDTO() }
    }
}
