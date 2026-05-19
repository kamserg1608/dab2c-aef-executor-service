package ru.sbrf.dab2c.executor.clients.configurator.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import ru.sbrf.dab2c.executor.clients.common.config.ConnectionPoolProperties
import ru.sbrf.dab2c.executor.clients.common.config.RetryProperties

/**
 * Configuration properties for EFS Adapter HTTP client.
 */
@ConfigurationProperties(prefix = "configutator")
data class ConfiguratorClientConfigurationProperties(
    val baseUrl: String,
    val connectionTimeout: Long,
    val requestTimeout: Long,
    val socketTimeout: Long,
    val pool: ConnectionPoolProperties,
    val retry: RetryProperties
)
