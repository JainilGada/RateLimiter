package xyz.sarva.ratelimiter.strategy

import xyz.sarva.ratelimiter.RateLimiter
import xyz.sarva.ratelimiter.RateLimiterConfig
import xyz.sarva.ratelimiter.RequestContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class FixedWindowRateLimiter(
    private val config: RateLimiterConfig
) : RateLimiter {

    private val requestMap = ConcurrentHashMap<String, RequestCounter>()

    override fun allowRequest(context: RequestContext): Boolean {
        val key = config.keyResolver(context)
        return key?.let { allowKey(it) } ?: false
    }

    override fun allowKey(key: String,): Boolean {
        val now = System.currentTimeMillis()
        val counter = requestMap.computeIfAbsent(key) {
            RequestCounter(AtomicLong(0), now)
        }

        val isAllowed = if (config.optimistic) {
            allowOptimistically(counter, now)
        } else {
            allowStrictly(counter, now)
        }

       // if (!isAllowed) println("Rate limit exceeded for key: $key")
        return isAllowed
    }

    private fun allowOptimistically(counter: RequestCounter, now: Long): Boolean {
        if (now - counter.windowStart >= config.windowSizeInMillis) {
            synchronized(counter) {
                // Re-check inside lock to avoid race
                if (now - counter.windowStart >= config.windowSizeInMillis) {
                    counter.windowStart = now
                    counter.count.set(1)
                    return true
                }
            }
        }

        return if (counter.count.get() < config.limit) {
            counter.count.getAndIncrement()
            true
        } else {
            handleLimitExceeded()
        }
    }

    private fun allowStrictly(counter: RequestCounter, now: Long): Boolean {
        synchronized(counter) {
            if (now - counter.windowStart >= config.windowSizeInMillis) {
                counter.windowStart = now
                counter.count.set(1)
                return true
            }

            return if (counter.count.get() < config.limit) {
                counter.count.getAndIncrement()
                true
            } else {
                handleLimitExceeded()
            }
        }
    }



    private fun handleLimitExceeded(): Boolean {
        return when (config.exceedStrategy) {
            RateLimiterConfig.LimitExceedStrategy.REJECT -> false
        }
    }

    private class RequestCounter(
        var count: AtomicLong,
        @Volatile var windowStart: Long
    )
}
