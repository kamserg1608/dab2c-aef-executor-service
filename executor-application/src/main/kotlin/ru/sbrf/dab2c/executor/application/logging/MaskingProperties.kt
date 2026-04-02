package ru.sbrf.dab2c.executor.application.logging

import org.springframework.boot.context.properties.ConfigurationProperties

/** Binding for `executor.masking.*` properties. */
@ConfigurationProperties(prefix = "executor.masking")
data class MaskingProperties(
    val keyMaskingEnabled: Boolean = true,
    val regexMaskingEnabled: Boolean = true,
    val regexPatterns: List<String> = emptyList()
)
