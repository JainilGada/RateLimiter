package xyz.sarva.ratelimiter

/**
 * Configuration used to construct different rate limiter strategies.
 */
data class RateLimiterConfig(
    val limit: Int,
    val windowSizeInMillis: Long,
    val strategyType: StrategyType = StrategyType.FIXED_WINDOW,
    val exceedStrategy: LimitExceedStrategy = LimitExceedStrategy.REJECT,
    val keyResolver: (RequestContext) -> String?,
    val optimistic: Boolean = false
) {
    /**
     * Enum representing different rate limiting algorithms.
     */
    enum class StrategyType {
        FIXED_WINDOW,
        TOKEN_BUCKET
        // TODO: SLIDING_WINDOW, LEAKY_BUCKET, etc.
    }

    /**
     * Enum representing the behavior when the request exceeds the limit.
     */
    enum class LimitExceedStrategy {
        REJECT,
       // TODO: RETRY_AFTER, DELAY
    }
}
