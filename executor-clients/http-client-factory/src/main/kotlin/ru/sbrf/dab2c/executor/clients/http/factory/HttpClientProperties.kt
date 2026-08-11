package ru.sbrf.dab2c.executor.clients.http.factory

import io.ktor.client.plugins.logging.LogLevel

/**
 * Configuration of a single outbound HTTP client.
 */
data class HttpClientProperties(
    val baseUrl: String,
    val connectionTimeout: Long,
    val requestTimeout: Long,
    val socketTimeout: Long,
    val pool: ConnectionPoolProperties,
    val retry: RetryProperties,
    val logLevel: LogLevel = LogLevel.ALL
)
