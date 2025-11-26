package ru.sbrf.ufs.dab2c.core.executor.shared.healthcheck

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.ufs.platform.healthcheck.HealthCheck

/**
 * Configuration for health checks.
 */
@Configuration
class HealthCheckConfiguration {

    @Bean
    internal fun appHealthCheck(): HealthCheck = ApplicationHealthCheck()
}
