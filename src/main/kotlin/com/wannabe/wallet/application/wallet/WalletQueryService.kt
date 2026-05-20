package com.wannabe.wallet.application.wallet

import com.wannabe.wallet.application.wallet.dto.TransactionResult
import com.wannabe.wallet.domain.wallet.WalletErrorCode
import com.wannabe.wallet.domain.wallet.WalletException
import com.wannabe.wallet.domain.wallet.WalletQueryStore
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WalletQueryService(
    private val walletQueryStore: WalletQueryStore,
    private val walletAssembler: WalletAssembler,
) {
    @Transactional(readOnly = true)
    fun getTransactions(walletId: String): List<TransactionResult> {
        if (!walletQueryStore.exists(walletId)) {
            throw WalletException(WalletErrorCode.WALLET_NOT_FOUND)
        }

        return walletQueryStore.findTransactions(walletId)
            .map { walletAssembler.toResult(it) }
    }
}
