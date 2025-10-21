package ru.sbrf.ufs.dab2c.core.executor.shared.healthcheck

import org.slf4j.Logger
import ru.sbrf.ufs.platform.healthcheck.Health
import ru.sbrf.ufs.platform.healthcheck.HealthCheck
import ru.sbrf.ufs.platform.logger.LoggerFactory

/**
 * Реализация мониторинга здоровья приложения.
 */
class ApplicationHealthCheck : HealthCheck {
    override fun check(): Health {
        LOGGER.info("Health:check()")
        return Health.OK
    }

    internal companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(
            ApplicationHealthCheck::class.java
        )
    }
}
