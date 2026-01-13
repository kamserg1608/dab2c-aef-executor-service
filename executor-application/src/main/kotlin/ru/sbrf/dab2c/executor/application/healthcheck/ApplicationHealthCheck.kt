package ru.sbrf.dab2c.executor.application.healthcheck

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

/**
 * Application health indicator implementation.
 */
@Component
class ApplicationHealthCheck : HealthIndicator {
    override fun health(): Health = Health.up().build()
}
