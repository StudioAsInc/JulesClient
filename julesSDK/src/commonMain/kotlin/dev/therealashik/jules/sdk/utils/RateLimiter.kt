package dev.therealashik.jules.sdk.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

/**
 * A simple token bucket rate limiter.
 * Allows [requestsPerMinute] requests per minute.
 */
class RateLimiter(private val requestsPerMinute: Int) {
    private val mutex = Mutex()
    private var tokens = requestsPerMinute.toDouble()
    private var lastRefillTime = Clock.System.now().toEpochMilliseconds()

    // tokens to add per millisecond
    private val refillRateMs = requestsPerMinute.toDouble() / 60_000.0

    /**
     * Acquires a token, suspending if necessary until a token is available.
     */
    suspend fun acquire() {
        if (requestsPerMinute <= 0) return // No limit

        mutex.withLock {
            refillTokens()

            if (tokens >= 1.0) {
                tokens -= 1.0
                return@withLock
            }

            // We need to wait for at least 1 token
            val tokensNeeded = 1.0 - tokens
            val waitTimeMs = (tokensNeeded / refillRateMs).toLong()

            if (waitTimeMs > 0) {
                delay(waitTimeMs)
                refillTokens()
            }
            tokens -= 1.0
        }
    }

    private fun refillTokens() {
        val now = Clock.System.now().toEpochMilliseconds()
        val timePassed = now - lastRefillTime

        if (timePassed > 0) {
            val tokensToAdd = timePassed * refillRateMs
            tokens = kotlin.math.min(requestsPerMinute.toDouble(), tokens + tokensToAdd)
            lastRefillTime = now
        }
    }
}
