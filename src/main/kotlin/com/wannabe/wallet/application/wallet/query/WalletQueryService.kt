package com.wannabe.wallet.application.wallet.query

import com.wannabe.wallet.application.wallet.assembler.WalletAssembler
import com.wannabe.wallet.application.wallet.dto.TransactionResult
import com.wannabe.wallet.domain.wallet.exception.WalletErrorCode
import com.wannabe.wallet.domain.wallet.exception.WalletException
import com.wannabe.wallet.domain.wallet.port.WalletQueryStore
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
