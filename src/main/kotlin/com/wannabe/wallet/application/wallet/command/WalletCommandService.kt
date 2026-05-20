package com.wannabe.wallet.application.wallet.command

import com.wannabe.wallet.application.wallet.dto.WithdrawalResult
import com.wannabe.wallet.domain.wallet.port.WalletLock
import com.wannabe.wallet.domain.wallet.service.WalletDomainService
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class WalletCommandService(
    private val walletLock: WalletLock,
    private val walletDomainService: WalletDomainService,
    private val walletWithdrawalProcessor: WalletWithdrawalProcessor,
) {
    fun withdraw(walletId: String, amount: BigDecimal, currency: String, transactionId: String): WithdrawalResult {
        walletDomainService.validateWithdrawalAmount(amount)

        return walletLock.execute(walletId) {
            walletWithdrawalProcessor.withdraw(walletId, amount, currency, transactionId)
        }
    }
}
