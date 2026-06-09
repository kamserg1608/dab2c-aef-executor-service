package ru.sbrf.dab2c.executor.clients.http.factory

/**
 * Retry configuration properties for HTTP clients.
 */
data class RetryProperties(
    val maxRetries: Int,
    val delay: Long,
    val maxDelay: Long,
    val multiplier: Double,
    val statusCodes: List<Int>
)
