package xyz.sarva.ratelimiter.test

import xyz.sarva.ratelimiter.RateLimiterConfig
import xyz.sarva.ratelimiter.RequestContext
import xyz.sarva.ratelimiter.strategy.RateLimiterFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

fun runBenchmarks() {
    val users = listOf("user1") // One user = worst-case for contention
    val threadCount = 8

    for (testCase in testCases) {
        listOf(false, true).forEach { isOptimistic ->
            val config = RateLimiterConfig(
                limit = testCase.limit,
                windowSizeInMillis = testCase.windowSizeInMillis,
                strategyType = RateLimiterConfig.StrategyType.FIXED_WINDOW,
                exceedStrategy = RateLimiterConfig.LimitExceedStrategy.REJECT,
                keyResolver = { it.userId },
                optimistic = isOptimistic
            )

            val rateLimiter = RateLimiterFactory.create(config)
            val executor = Executors.newFixedThreadPool(threadCount)
            val allowedCounter = AtomicInteger(0)
            val latch = CountDownLatch(testCase.totalRequests)

            val start = System.currentTimeMillis()

            repeat(testCase.totalRequests) { i ->
                executor.submit {
                    val user = users[i % users.size]
                    val allowed = rateLimiter.allowRequest(RequestContext(userId = user))
                    if (allowed) allowedCounter.incrementAndGet()
                    latch.countDown()
                }
            }

            latch.await()
            val end = System.currentTimeMillis()
            executor.shutdown()

            println(
                """
                ${"-".repeat(60)}
                Test: ${testCase.name}
                Comment: ${testCase.comment}
                Mode: ${if (isOptimistic) "Optimistic" else "Strict"}
                Limit: ${testCase.limit}
                Window Size: ${testCase.windowSizeInMillis} ms
                Total Requests: ${testCase.totalRequests}
                Allowed: ${allowedCounter.get()}
                Rejected: ${testCase.totalRequests - allowedCounter.get()}
                Time Taken: ${end - start} ms
                ${"-".repeat(60)}
                """.trimIndent()
            )
        }
    }
}


/**

------------------------------------------------------------
Test: Low limit, short window
Comment: Tests short burst windows with low quota. High contention, tests race conditions.
Mode: Strict
Limit: 1000
Window Size: 100 ms
Total Requests: 5000000
Allowed: 11000
Rejected: 4989000
Time Taken: 1296 ms
------------------------------------------------------------
------------------------------------------------------------
Test: Low limit, short window
Comment: Tests short burst windows with low quota. High contention, tests race conditions.
Mode: Optimistic
Limit: 1000
Window Size: 100 ms
Total Requests: 5000000
Allowed: 7000
Rejected: 4993000
Time Taken: 674 ms
------------------------------------------------------------
------------------------------------------------------------
Test: High limit, short window
Comment: Short window but large quota. Tests optimistic under high concurrency with few resets.
Mode: Strict
Limit: 500000
Window Size: 100 ms
Total Requests: 1000000
Allowed: 800803
Rejected: 199197
Time Taken: 129 ms
------------------------------------------------------------
------------------------------------------------------------
Test: High limit, short window
Comment: Short window but large quota. Tests optimistic under high concurrency with few resets.
Mode: Optimistic
Limit: 500000
Window Size: 100 ms
Total Requests: 1000000
Allowed: 622250
Rejected: 377750
Time Taken: 120 ms
------------------------------------------------------------
------------------------------------------------------------
Test: Low limit, long window
Comment: Sustained low QPS. Ideal for accurate quota tracking. Strict will dominate.
Mode: Strict
Limit: 100
Window Size: 60000 ms
Total Requests: 500000
Allowed: 100
Rejected: 499900
Time Taken: 66 ms
------------------------------------------------------------
------------------------------------------------------------
Test: Low limit, long window
Comment: Sustained low QPS. Ideal for accurate quota tracking. Strict will dominate.
Mode: Optimistic
Limit: 100
Window Size: 60000 ms
Total Requests: 500000
Allowed: 100
Rejected: 499900
Time Taken: 62 ms
------------------------------------------------------------
------------------------------------------------------------
Test: High limit, long window
Comment: Real-world API burst usage. Optimistic should shine here due to low lock overhead.
Mode: Strict
Limit: 1000000
Window Size: 60000 ms
Total Requests: 5000000
Allowed: 1000000
Rejected: 4000000
Time Taken: 777 ms
------------------------------------------------------------
------------------------------------------------------------
Test: High limit, long window
Comment: Real-world API burst usage. Optimistic should shine here due to low lock overhead.
Mode: Optimistic
Limit: 1000000
Window Size: 60000 ms
Total Requests: 5000000
Allowed: 1000000
Rejected: 4000000
Time Taken: 604 ms
------------------------------------------------------------
------------------------------------------------------------
Test: Moderate limit, moderate window
Comment: Balanced use-case. Measures both performance and correctness under moderate load.
Mode: Strict
Limit: 10000
Window Size: 5000 ms
Total Requests: 1000000
Allowed: 10000
Rejected: 990000
Time Taken: 122 ms
------------------------------------------------------------
------------------------------------------------------------
Test: Moderate limit, moderate window
Comment: Balanced use-case. Measures both performance and correctness under moderate load.
Mode: Optimistic
Limit: 10000
Window Size: 5000 ms
Total Requests: 1000000
Allowed: 10000
Rejected: 990000
Time Taken: 112 ms
------------------------------------------------------------

Recommendations
Use Case Type	Recommended Mode
💥 Short bursts, low limits	Strict (accuracy)
⚠️ Short bursts, high limits	Strict or Adaptive
✅ Long windows, high QPS	Optimistic (speed)
⚖️ Balanced apps (5s/10s windows)	Optimistic (safe)

 * */