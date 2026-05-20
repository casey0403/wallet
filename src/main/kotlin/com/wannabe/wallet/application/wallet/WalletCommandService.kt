package com.wannabe.wallet.application.wallet

import com.wannabe.wallet.application.wallet.dto.WithdrawalResult
import com.wannabe.wallet.domain.wallet.WalletDomainService
import com.wannabe.wallet.infrastructure.adapter.WalletRedisLock
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class WalletCommandService(
    private val walletRedisLock: WalletRedisLock,
    private val walletDomainService: WalletDomainService,
    private val walletWithdrawalProcessor: WalletWithdrawalProcessor,
) {
    fun withdraw(walletId: String, amount: BigDecimal, currency: String, transactionId: String): WithdrawalResult {
        walletDomainService.validateWithdrawalAmount(amount)

        return walletRedisLock.execute(walletId) {
            walletWithdrawalProcessor.withdraw(walletId, amount, currency, transactionId)
        }
    }
}
