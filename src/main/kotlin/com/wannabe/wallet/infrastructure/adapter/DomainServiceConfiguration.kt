package com.wannabe.wallet.infrastructure.adapter

import com.wannabe.wallet.domain.wallet.service.WalletDomainService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DomainServiceConfiguration {
    @Bean
    fun walletDomainService(): WalletDomainService {
        return WalletDomainService()
    }
}
