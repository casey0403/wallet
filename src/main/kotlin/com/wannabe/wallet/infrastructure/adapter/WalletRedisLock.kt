package com.wannabe.wallet.infrastructure.adapter

import com.wannabe.wallet.domain.wallet.WalletErrorCode
import com.wannabe.wallet.domain.wallet.WalletException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

@Component
class WalletRedisLock(
    private val redisTemplate: StringRedisTemplate,
) {
    private val unlockScript = DefaultRedisScript(
        """
        if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
        end
        return 0
        """.trimIndent(),
        Long::class.java,
    )

    fun <T> execute(walletId: String, block: () -> T): T {
        val key = "wallet:withdraw:$walletId"
        val token = UUID.randomUUID().toString()
        val acquired = tryLock(key, token, waitTimeout = Duration.ofSeconds(10), leaseTime = Duration.ofSeconds(5))

        if (!acquired) {
            throw WalletException(WalletErrorCode.WALLET_BUSY)
        }

        return try {
            block()
        } finally {
            redisTemplate.execute(unlockScript, listOf(key), token)
        }
    }

    private fun tryLock(
        key: String,
        token: String,
        waitTimeout: Duration,
        leaseTime: Duration,
    ): Boolean {
        val deadline = System.nanoTime() + waitTimeout.toNanos()
        while (System.nanoTime() < deadline) {
            val acquired = redisTemplate.opsForValue().setIfAbsent(key, token, leaseTime) == true
            if (acquired) {
                return true
            }
            Thread.sleep(20)
        }
        return false
    }
}
