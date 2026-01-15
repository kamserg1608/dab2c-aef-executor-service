package ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import ru.sbrf.dab2c.executor.clients.common.config.ConnectionPoolProperties
import ru.sbrf.dab2c.executor.clients.common.config.RetryProperties

/**
 * Configuration properties for GigaVoice Agent HTTP client.
 */
@ConfigurationProperties(prefix = "giga.voice.agent.client")
data class GigaVoiceAgentClientConfigurationProperties(
    val baseUrl: String,
    val connectionTimeout: Long,
    val requestTimeout: Long,
    val socketTimeout: Long,
    val pool: ConnectionPoolProperties,
    val retry: RetryProperties
)
