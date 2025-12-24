package ru.sbrf.dab2c.executor.application.healthcheck

import org.springframework.stereotype.Component
import ru.sbrf.ufs.platform.healthcheck.Health
import ru.sbrf.ufs.platform.healthcheck.HealthCheck

/**
 * Реализация мониторинга здоровья приложения.
 */
@Component
class ApplicationHealthCheck : HealthCheck {
    override fun check(): Health = Health.OK
}
