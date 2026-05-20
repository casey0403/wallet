package com.wannabe.wallet.application.wallet.command

import com.wannabe.wallet.application.wallet.dto.WithdrawalResultDTO
import com.wannabe.wallet.domain.wallet.vo.Money
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
    fun withdraw(walletId: String, amount: BigDecimal, currency: String, transactionId: String): WithdrawalResultDTO {
        val money = Money(
            amount = amount,
            currency = currency,
        )
        walletDomainService.validateWithdrawalAmount(money)

        return walletLock.execute(walletId = walletId) {
            walletWithdrawalProcessor.withdraw(
                walletId = walletId,
                money = money,
                transactionId = transactionId,
            )
        }
    }
}
