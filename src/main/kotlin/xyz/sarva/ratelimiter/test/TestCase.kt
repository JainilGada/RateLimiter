package xyz.sarva.ratelimiter.test

data class TestCase(
    val name: String,
    val windowSizeInMillis: Long,
    val limit: Int,
    val totalRequests: Int,
    val comment: String
)

enum class Mode(val label: String) {
    STRICT("Strict"),
    OPTIMISTIC_LOCK("OptimisticWithLock"),
    OPTIMISTIC_CAS("OptimisticCAS")
}

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


val testCases2 = listOf(
    TestCase("Low limit, short window", 100, 1000, 5_000_000, "High contention, burst load"),
    TestCase("High limit, short window", 100, 500_000, 1_000_000, "Few resets, high QPS"),
    TestCase("Low limit, long window", 60_000, 100, 500_000, "Sustained low QPS"),
    TestCase("High limit, long window", 60_000, 1_000_000, 5_000_000, "API quotas, long-running"),
    TestCase("Moderate, moderate", 5000, 10_000, 1_000_000, "Balanced, realistic case")
)

val modes = listOf(
    Mode.STRICT,
    Mode.OPTIMISTIC_LOCK,
    Mode.OPTIMISTIC_CAS
)
