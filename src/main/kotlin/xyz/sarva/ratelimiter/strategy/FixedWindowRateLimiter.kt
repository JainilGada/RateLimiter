package xyz.sarva.ratelimiter.strategy

import xyz.sarva.ratelimiter.RateLimiter
import xyz.sarva.ratelimiter.RateLimiterConfig
import xyz.sarva.ratelimiter.RequestContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class FixedWindowRateLimiter(
    private val config: RateLimiterConfig
) : RateLimiter {

    private val strictMap = ConcurrentHashMap<String, StrictRequestCounter>()
    private val optimisticMap = ConcurrentHashMap<String, OptimisticRequestCounter>()

    override fun allowRequest(context: RequestContext): Boolean {
        val key = config.keyResolver(context)
        return key?.let { allowKey(it) } ?: false
    }

    override fun allowKey(key: String): Boolean {
        val now = System.currentTimeMillis()

        return when {
            config.optimistic && config.useCAS -> {
                val counter = optimisticMap.computeIfAbsent(key) {
                    OptimisticRequestCounter(AtomicLong(0), AtomicLong(now))
                }
                allowOptimisticallyCas(counter, now)
            }

            config.optimistic -> {
                val counter = optimisticMap.computeIfAbsent(key) {
                    OptimisticRequestCounter(AtomicLong(0), AtomicLong(now))
                }
                allowOptimisticallyWithLock(counter, now)
            }

            else -> {
                val counter = strictMap.computeIfAbsent(key) {
                    StrictRequestCounter(0, now)
                }
                allowStrictlyAssumingOneThread(counter, now)
            }
        }
    }

    private fun allowStrictlyAssumingOneThread(counter: StrictRequestCounter, now: Long): Boolean {
            if (now - counter.windowStart >= config.windowSizeInMillis) {
                counter.windowStart = now
                counter.count = 1
                return true
            }

            return if (counter.count < config.limit) {
                counter.count++
                true
            } else {
                handleLimitExceeded()
            }
    }

    private fun allowStrictly(counter: StrictRequestCounter, now: Long): Boolean {
        synchronized(counter) {
            if (now - counter.windowStart >= config.windowSizeInMillis) {
                counter.windowStart = now
                counter.count = 1
                return true
            }

            return if (counter.count < config.limit) {
                counter.count++
                true
            } else {
                handleLimitExceeded()
            }
        }
    }

    private fun allowOptimisticallyWithLock(counter: OptimisticRequestCounter, now: Long): Boolean {
        if (now - counter.windowStart.get() >= config.windowSizeInMillis) {
            synchronized(counter) {
                if (now - counter.windowStart.get() >= config.windowSizeInMillis) {
                    counter.windowStart.set(now)
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

    private fun allowOptimisticallyCas(counter: OptimisticRequestCounter, now: Long): Boolean {
        val currentWindowStart = counter.windowStart.get()
        val windowEnd = currentWindowStart + config.windowSizeInMillis

        // Window expired: try to reset
        if (now >= windowEnd) {
            /*
            * Do not retry in case of failure
            * */
            val won = counter.windowStart.compareAndSet(currentWindowStart, now)
            if (won) {
                // Successfully reset window
                counter.count.set(1)
                return true
            }
            // Someone else reset — fall through to normal path
        }

        // Count check without retrying anything
        return if (counter.count.get() < config.limit) {
            counter.count.getAndIncrement()
            true
        } else {
            handleLimitExceeded()
        }
    }


    private fun handleLimitExceeded(): Boolean {
        return when (config.exceedStrategy) {
            RateLimiterConfig.LimitExceedStrategy.REJECT -> false
        }
    }

    // ---- Separate counter classes ----

    private class StrictRequestCounter(
        var count: Long,
        var windowStart: Long
    )

    private class OptimisticRequestCounter(
        val count: AtomicLong,
        val windowStart: AtomicLong
    )
}
