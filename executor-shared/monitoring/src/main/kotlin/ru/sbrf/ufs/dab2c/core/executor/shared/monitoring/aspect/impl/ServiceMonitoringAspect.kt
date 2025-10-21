package ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.aspect.impl

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.annotation.SystemServiceMonitored
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.aspect.AbstractMonitoringAspect
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricOrigin
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricPath
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.MonitoringServiceAdapter

/**
 * Monitoring aspect for system services that don`t require specific monitoring.
 * Method should be annotated [SystemServiceMonitored] to be monitored.
 */
@Aspect
class ServiceMonitoringAspect(
    localTimeProvider: LocalTimeProvider,
    monitoringServiceAdapter: MonitoringServiceAdapter
) : AbstractMonitoringAspect<SystemServiceMonitored>(
    monitoringServiceAdapter,
    localTimeProvider
) {

    @Around("@annotation(config)")
    override fun advice(joinPoint: ProceedingJoinPoint, config: SystemServiceMonitored): Any? {
        val metricPath = MetricPath(
            origin = MetricOrigin.SYSTEM,
            service = config.service,
            action = config.action
        )
        return executionWrapper(joinPoint, listOf(metricPath))
    }
}
