package ru.sbrf.dab2c.executor.application.warmup

import org.springframework.boot.context.properties.ConfigurationProperties

/** Binding for `executor.warmup.*` properties. */
@ConfigurationProperties(prefix = "executor.warmup")
data class WarmupProperties(
    val enabled: Boolean = true
)
