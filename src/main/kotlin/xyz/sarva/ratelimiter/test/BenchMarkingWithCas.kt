package xyz.sarva.ratelimiter.test

import xyz.sarva.ratelimiter.RateLimiterConfig
import xyz.sarva.ratelimiter.RequestContext
import xyz.sarva.ratelimiter.strategy.RateLimiterFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger


fun runBenchmarksWithCas() {
    val users = listOf("user1")
    val threadCount = 8

    for (testCase in testCases) {
        for (mode in modes) {
            val config = RateLimiterConfig(
                limit = testCase.limit,
                windowSizeInMillis = testCase.windowSizeInMillis,
                strategyType = RateLimiterConfig.StrategyType.FIXED_WINDOW,
                exceedStrategy = RateLimiterConfig.LimitExceedStrategy.REJECT,
                keyResolver = { it.userId },
                optimistic = mode != Mode.STRICT,
                useCAS = mode == Mode.OPTIMISTIC_CAS
            )

            val rateLimiter = RateLimiterFactory.create(config)
            val executor = Executors.newFixedThreadPool(threadCount)
            val allowedCounter = AtomicInteger(0)
            val latch = CountDownLatch(testCase.totalRequests)

            val start = System.currentTimeMillis()

            repeat(testCase.totalRequests) { i ->
                executor.submit {
                    val userId = users[i % users.size]
                    val allowed = rateLimiter.allowRequest(RequestContext(userId = userId))
                    if (allowed) allowedCounter.incrementAndGet()
                    latch.countDown()
                }
            }

            latch.await()
            val end = System.currentTimeMillis()
            executor.shutdown()

            val allowed = allowedCounter.get()
            val rejected = testCase.totalRequests - allowed

            println(
                """
                ${"-".repeat(60)}
                Test: ${testCase.name}
                Comment: ${testCase.comment}
                Mode: ${mode.label}
                Limit: ${testCase.limit}
                Window Size: ${testCase.windowSizeInMillis} ms
                Total Requests: ${testCase.totalRequests}
                ✅ Allowed Requests: $allowed
                ❌ Rejected Requests: $rejected
                ⏱️ Time Taken: ${end - start} ms
                ${"-".repeat(60)}
                """.trimIndent()
            )
        }
    }
}