package ru.sbrf.dab2c.executor.clients.http.factory

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configurations of all outbound HTTP clients keyed by client name.
 */
@ConfigurationProperties(prefix = "http")
data class HttpClientsProperties(
    val clients: Map<String, HttpClientProperties> = emptyMap()
)
