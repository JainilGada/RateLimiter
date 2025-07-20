package xyz.sarva.ratelimiter


data class RequestContext(
    val userId: String? = null,
    val ipAddress: String? = null,
    val tenantId: String? = null,
    val extra: Map<String, String> = emptyMap()
)