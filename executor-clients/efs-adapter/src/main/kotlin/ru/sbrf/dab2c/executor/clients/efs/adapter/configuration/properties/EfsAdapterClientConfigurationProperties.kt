package ru.sbrf.dab2c.executor.clients.efs.adapter.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for EFS Adapter HTTP client.
 */
@ConfigurationProperties(prefix = "efs.adapter")
data class EfsAdapterClientConfigurationProperties(
    val baseUrl: String = "http://localhost:8080",
    val connectionTimeout: Long = 30_000,
    val requestTimeout: Long = 30_000
)
