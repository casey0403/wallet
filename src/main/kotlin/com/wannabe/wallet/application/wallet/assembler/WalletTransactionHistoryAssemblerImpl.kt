package com.wannabe.wallet.application.wallet.assembler

import com.wannabe.wallet.application.common.ApplicationService
import com.wannabe.wallet.application.wallet.dto.TransactionDTO
import com.wannabe.wallet.application.wallet.dto.toTransactionDTO
import com.wannabe.wallet.domain.wallet.error.WalletErrorCode
import com.wannabe.wallet.domain.wallet.exception.WalletException
import com.wannabe.wallet.domain.wallet.port.WalletQueryStore
import org.springframework.transaction.annotation.Transactional

@ApplicationService
class WalletTransactionHistoryAssemblerImpl(
    private val walletQueryStore: WalletQueryStore,
) : WalletTransactionHistoryAssembler {
    @Transactional(readOnly = true)
    override fun getTransactions(walletId: String): List<TransactionDTO> {
        if (!walletQueryStore.exists(walletId)) {
            throw WalletException(WalletErrorCode.WALLET_NOT_FOUND)
        }

        return walletQueryStore.findTransactions(walletId)
            .map { it.toTransactionDTO() }
    }
}
