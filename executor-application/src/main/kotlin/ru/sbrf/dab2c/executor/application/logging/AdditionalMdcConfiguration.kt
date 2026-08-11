package ru.sbrf.dab2c.executor.application.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.logging.AdditionalMdcConfig

private val logger = KotlinLogging.logger {}

/** Wires [AdditionalMdcProperties] into the static [AdditionalMdcConfig] holder at startup. */
@Configuration
@EnableConfigurationProperties(AdditionalMdcProperties::class)
class AdditionalMdcConfiguration(private val properties: AdditionalMdcProperties) {

    /** Populates [AdditionalMdcConfig] so MDC context includes the configured keys. */
    @PostConstruct
    fun init() {
        AdditionalMdcConfig.configure(properties.additionalMdcKeys)
        logger.info { "Additional MDC keys configured: ${properties.additionalMdcKeys}" }
    }
}
