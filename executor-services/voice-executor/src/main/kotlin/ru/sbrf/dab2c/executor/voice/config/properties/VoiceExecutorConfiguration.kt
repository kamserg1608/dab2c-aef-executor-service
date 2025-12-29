package ru.sbrf.dab2c.executor.voice.config.properties

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(value = [
    VoiceExecutorConfigurationProperties::class
])
class VoiceExecutorConfiguration