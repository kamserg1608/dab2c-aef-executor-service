@file:Suppress("DEPRECATION")

package ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import ru.sbrf.ufs.dab2c.core.executor.shared.annotations.ProdProfileOnly
import ru.sbrf.ufs.dab2c.core.executor.shared.annotations.StubProfileOnly
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.aspect.impl.ServiceMonitoringAspect
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.MonitoringServiceAdapter
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.impl.CoreMonitoringServiceMock
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.impl.MonitoringServiceAdapterImpl
import ru.sbrf.ufs.platform.monitoring.CoreMonitoringService

/**
 * Base monitoring configuration.
 */
@Configuration
@EnableAspectJAutoProxy
class MonitoringConfiguration {

    @Bean
    @ProdProfileOnly
    internal fun monitoringServiceAdapter(
        monitoringService: CoreMonitoringService
    ): MonitoringServiceAdapter = MonitoringServiceAdapterImpl(monitoringService)

    @Bean
    @StubProfileOnly
    internal fun monitoringServiceAdapterStub(): MonitoringServiceAdapter =
        MonitoringServiceAdapterImpl(CoreMonitoringServiceMock())

    @Bean
    internal fun serviceMonitoringAspect(
        localTimeProvider: LocalTimeProvider,
        monitoringServiceAdapter: MonitoringServiceAdapter
    ) = ServiceMonitoringAspect(
        localTimeProvider,
        monitoringServiceAdapter
    )
}
