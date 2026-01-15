package ru.sbrf.dab2c.executor.clients.efs.adapter.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import ru.sbrf.dab2c.executor.clients.common.config.ConnectionPoolProperties
import ru.sbrf.dab2c.executor.clients.common.config.RetryProperties

/**
 * Configuration properties for EFS Adapter HTTP client.
 */
@ConfigurationProperties(prefix = "efs.adapter")
data class EfsAdapterClientConfigurationProperties(
    val baseUrl: String,
    val connectionTimeout: Long,
    val requestTimeout: Long,
    val socketTimeout: Long,
    val pool: ConnectionPoolProperties,
    val retry: RetryProperties
)
