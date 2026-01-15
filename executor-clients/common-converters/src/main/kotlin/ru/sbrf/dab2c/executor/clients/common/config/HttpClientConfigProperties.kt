package ru.sbrf.dab2c.executor.clients.common.config

/**
 * Connection pool configuration properties for HTTP clients.
 */
data class ConnectionPoolProperties(
    val maxConnections: Int
)

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
