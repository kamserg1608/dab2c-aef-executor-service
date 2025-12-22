package ru.sbrf.ufs.dab2c.core.integration.healthcheck;

import org.springframework.stereotype.Component;
import ru.sbrf.ufs.platform.healthcheck.Health;
import ru.sbrf.ufs.platform.healthcheck.HealthCheck;

/**
 * Реализация мониторинга здоровья приложения
 */
@Component
public class ApplicationHealthCheck implements HealthCheck {

    @Override
    public Health check() {
        return Health.OK;
    }
}
