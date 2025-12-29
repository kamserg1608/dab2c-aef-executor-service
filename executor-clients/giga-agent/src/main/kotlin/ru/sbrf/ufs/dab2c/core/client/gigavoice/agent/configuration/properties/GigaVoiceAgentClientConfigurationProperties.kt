package ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "giga.voice.agent.client")
data class GigaVoiceAgentClientConfigurationProperties(
    val baseUrl: String = "http://localhost:8080",
    val connectionTimeout: Long = 120_000,
    val requestTimeout: Long = 120_000
)