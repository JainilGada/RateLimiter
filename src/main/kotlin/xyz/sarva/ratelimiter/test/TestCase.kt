package xyz.sarva.ratelimiter.test

data class TestCase(
    val name: String,
    val windowSizeInMillis: Long,
    val limit: Int,
    val totalRequests: Int,
    val comment: String
)

val testCases = listOf(
    TestCase(
        name = "Low limit, short window",
        windowSizeInMillis = 100,
        limit = 1000,
        totalRequests = 5_000_000,
        comment = "Tests short burst windows with low quota. High contention, tests race conditions."
    ),
    TestCase(
        name = "High limit, short window",
        windowSizeInMillis = 100,
        limit = 500_000,
        totalRequests = 1_000_000,
        comment = "Short window but large quota. Tests optimistic under high concurrency with few resets."
    ),
    TestCase(
        name = "Low limit, long window",
        windowSizeInMillis = 60_000,
        limit = 100,
        totalRequests = 500_000,
        comment = "Sustained low QPS. Ideal for accurate quota tracking. Strict will dominate."
    ),
    TestCase(
        name = "High limit, long window",
        windowSizeInMillis = 60_000,
        limit = 1_000_000,
        totalRequests = 5_000_000,
        comment = "Real-world API burst usage. Optimistic should shine here due to low lock overhead."
    ),
    TestCase(
        name = "Moderate limit, moderate window",
        windowSizeInMillis = 5_000,
        limit = 10_000,
        totalRequests = 1_000_000,
        comment = "Balanced use-case. Measures both performance and correctness under moderate load."
    )
)
