package dev.therealashik.jules.sdk.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertTrue

class RateLimiterTest {

    @Test
    fun testRateLimiterDelaysWhenTokensDepleted() = runBlocking {
        // Limit: 600 requests per minute -> 10 requests per second -> 1 token per 100ms
        // This is fast enough for a real-time test.
        val requestsPerMinute = 600
        val rateLimiter = RateLimiter(requestsPerMinute)

        val startTimeMs = Clock.System.now().toEpochMilliseconds()

        // Exhaust all tokens
        for (i in 1..requestsPerMinute) {
            rateLimiter.acquire()
        }

        val exhaustedTimeMs = Clock.System.now().toEpochMilliseconds()

        // This should take practically 0 time as we are just draining the bucket initially
        assertTrue(exhaustedTimeMs - startTimeMs < 500, "Draining tokens should be fast")

        // Now we should have 0 tokens. The next acquire should wait for a token.
        // It takes 100ms to replenish 1 token at 600 requests per minute.
        val timeBeforeWait = Clock.System.now().toEpochMilliseconds()

        rateLimiter.acquire()

        val timeAfterWait = Clock.System.now().toEpochMilliseconds()
        val timeDiff = timeAfterWait - timeBeforeWait

        // Check that delay was applied (roughly 100ms, using 50ms as lower bound for test stability)
        assertTrue(timeDiff >= 50, "Should have delayed for at least ~50ms, but delayed for $timeDiff ms")
    }

    @Test
    fun testRateLimiterUnlimited() = runBlocking {
        // 0 means no limit
        val rateLimiter = RateLimiter(0)

        val startTime = Clock.System.now().toEpochMilliseconds()
        for (i in 1..100) {
            rateLimiter.acquire()
        }
        val endTime = Clock.System.now().toEpochMilliseconds()

        assertTrue(endTime - startTime < 500, "Unlimited rate limiter should not delay")
    }
}
