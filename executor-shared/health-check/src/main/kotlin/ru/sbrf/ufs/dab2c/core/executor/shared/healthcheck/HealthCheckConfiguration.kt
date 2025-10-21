package ru.sbrf.ufs.dab2c.core.executor.shared.healthcheck

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for health checks.
 */
@Configuration
class HealthCheckConfiguration {

    @Bean
    internal fun appHealthCheck() = ApplicationHealthCheck()
}
