package ru.sbrf.dab2c.executor.voice.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "executor.voice")
data class VoiceExecutorConfigurationProperties(
    val proxyMode: Boolean = false
)