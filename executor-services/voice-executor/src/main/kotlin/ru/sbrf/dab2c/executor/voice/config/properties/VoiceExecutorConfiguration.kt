package ru.sbrf.dab2c.executor.voice.config.properties

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Spring configuration for voice executor service.
 */
@Configuration
@EnableConfigurationProperties(
    value = [
        VoiceExecutorConfigurationProperties::class
    ]
)
class VoiceExecutorConfiguration
