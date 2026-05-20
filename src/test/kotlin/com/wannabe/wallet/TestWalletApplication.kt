package com.wannabe.wallet

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
    fromApplication<WalletApplication>().with(TestcontainersConfiguration::class).run(*args)
}
