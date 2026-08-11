package ru.sbrf.dab2c.executor.application.logging

import org.springframework.boot.context.properties.ConfigurationProperties

/** Binding for `executor.additional-mdc-keys` properties. */
@ConfigurationProperties(prefix = "executor")
data class AdditionalMdcProperties(
    val additionalMdcKeys: Map<String, String> = emptyMap()
)
