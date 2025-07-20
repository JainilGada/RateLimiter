package xyz.sarva.ratelimiter.strategy

import xyz.sarva.ratelimiter.RateLimiter
import xyz.sarva.ratelimiter.RateLimiterConfig

object RateLimiterFactory {
    fun create(config: RateLimiterConfig): RateLimiter {
        return when (config.strategyType) {
            RateLimiterConfig.StrategyType.FIXED_WINDOW -> FixedWindowRateLimiter(config)
            // Future implementations
            RateLimiterConfig.StrategyType.TOKEN_BUCKET -> TODO("Token Bucket strategy not implemented yet")
            // Add more strategies like SLIDING_WINDOW, LEAKY_BUCKET, etc.
        }
    }
}