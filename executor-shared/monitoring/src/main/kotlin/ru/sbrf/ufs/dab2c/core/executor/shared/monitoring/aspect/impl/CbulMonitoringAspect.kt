package ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.aspect.impl

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.annotation.CbulMonitored
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.aspect.AbstractMonitoringAspect
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricOrigin
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricPath
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.MonitoringServiceAdapter

/**
 * Monitoring aspect for CBUL services.
 * Method should be annotated [CbulMonitored] to be monitored.
 */
@Aspect
class CbulMonitoringAspect(
    localTimeProvider: LocalTimeProvider,
    monitoringServiceAdapter: MonitoringServiceAdapter
) : AbstractMonitoringAspect<CbulMonitored>(
    monitoringServiceAdapter,
    localTimeProvider
) {

    @Around("@annotation(config)")
    override fun advice(joinPoint: ProceedingJoinPoint, config: CbulMonitored): Any? {
        val metricPath = MetricPath(
            origin = MetricOrigin.OUTBOUND,
            service = CBUL_SERVICE_NAME,
            action = config.action
        )
        return executionWrapper(joinPoint, listOf(metricPath))
    }

    /**
     * Companion object.
     */
    companion object {
        /** Cul service name. */
        const val CBUL_SERVICE_NAME = "CBUL"
    }
}
