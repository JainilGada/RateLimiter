package xyz.sarva.ratelimiter

/**
 * A contract for implementing different rate limiting strategies.
 */
interface RateLimiter {
    /**
     * Checks whether a request for the given context should be allowed or not.
     *
     * @param context the user/IP/tenant info
     * @return true if the request is within the allowed limit; false otherwise
     */
    fun allowRequest(context: RequestContext): Boolean

    /**
     * Checks whether a request with the given key should be allowed or not.
     *
     * @param key a unique key for the request
     * @return true if the request is within the allowed limit; false otherwise
     */
    fun allowKey(key: String): Boolean
}
