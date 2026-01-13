package ru.sbrf.dab2c.executor.voice.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for voice executor service.
 */
@ConfigurationProperties(prefix = "executor.voice")
data class VoiceExecutorConfigurationProperties(
    val proxyMode: Boolean = true
)
