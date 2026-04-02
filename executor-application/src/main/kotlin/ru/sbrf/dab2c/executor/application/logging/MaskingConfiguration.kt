package ru.sbrf.dab2c.executor.application.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

private val logger = KotlinLogging.logger {}

/** Wires [MaskingProperties] into the static [MaskingConfig] holder at startup. */
@Configuration
@EnableConfigurationProperties(MaskingProperties::class)
class MaskingConfiguration(private val properties: MaskingProperties) {

    /** Populates [MaskingConfig] so Logback components can read the settings. */
    @PostConstruct
    fun init() {
        MaskingConfig.configure(
            keyEnabled = properties.keyMaskingEnabled,
            regexEnabled = properties.regexMaskingEnabled,
            patterns = properties.regexPatterns
        )
        logger.info {
            "Masking configured: key-masking=${properties.keyMaskingEnabled}, " +
                "regex-masking=${properties.regexMaskingEnabled}, " +
                "regex-patterns=${properties.regexPatterns.size}"
        }
    }
}
