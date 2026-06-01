package ru.sbrf.dab2c.executor.clients.iag.iag.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import ru.sbrf.dab2c.executor.clients.common.config.ConnectionPoolProperties
import ru.sbrf.dab2c.executor.clients.common.config.RetryProperties

/**
 * Configuration properties for IAG HTTP client.
 */
@ConfigurationProperties(prefix = "iag.client")
data class IagClientConfigurationProperties(
    val baseUrl: String,
    val connectionTimeout: Long,
    val requestTimeout: Long,
    val socketTimeout: Long,
    val pool: ConnectionPoolProperties,
    val retry: RetryProperties
)
