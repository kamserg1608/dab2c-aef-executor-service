package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.aspect.impl.CbulMonitoringAspect
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.config.MonitoringConfiguration
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.MonitoringServiceAdapter

/**
 * Monitoring configuration for cbul.
 */
@Configuration
@Import(
    MonitoringConfiguration::class
)
class CbulMonitoringConfiguration {

    @Bean
    @ConditionalOnBean(MonitoringServiceAdapter::class)
    internal fun cbulMonitoringAspect(
        localTimeProvider: LocalTimeProvider,
        monitoringServiceAdapter: MonitoringServiceAdapter
    ) = CbulMonitoringAspect(localTimeProvider, monitoringServiceAdapter)
}
