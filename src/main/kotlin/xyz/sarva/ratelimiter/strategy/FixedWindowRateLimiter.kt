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
            RequestCounter(AtomicLong(0), AtomicLong(now))
        }

        val isAllowed = when {
            config.optimistic && config.useCAS -> allowOptimisticallyCas(counter, now)
            config.optimistic -> allowOptimisticallyWithLock(counter, now)
            else -> allowStrictly(counter, now)
        }

       // if (!isAllowed) println("Rate limit exceeded for key: $key")
        return isAllowed
    }

    private fun allowOptimisticallyCas(counter: RequestCounter, now: Long): Boolean {
        val windowStart = counter.windowStart.get()
        val windowEnd = windowStart + config.windowSizeInMillis

        if (now >= windowEnd) {
            if (counter.windowStart.compareAndSet(windowStart, now)) {
                counter.count.set(1)
                return true
            }
            // CAS failed: another thread already reset the window
        }

        return if (counter.count.get() < config.limit) {
            counter.count.getAndIncrement()
            true
        } else {
            handleLimitExceeded()
        }
    }


    private fun allowOptimisticallyWithLock(counter: RequestCounter, now: Long): Boolean {
        if (now - counter.windowStart.get() >= config.windowSizeInMillis) {
            synchronized(counter) {
                // Re-check inside lock to avoid race
                if (now - counter.windowStart.get() >= config.windowSizeInMillis) {
                    counter.windowStart = AtomicLong(now)
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
            if (now - counter.windowStart.get() >= config.windowSizeInMillis) {
                counter.windowStart = AtomicLong(now)
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
        @Volatile var windowStart: AtomicLong
    )
}
