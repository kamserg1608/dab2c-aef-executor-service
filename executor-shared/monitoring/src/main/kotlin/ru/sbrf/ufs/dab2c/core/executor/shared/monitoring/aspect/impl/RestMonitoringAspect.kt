package ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.aspect.impl

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.api.CallerNameExtractor
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.annotation.RestMonitored
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.aspect.AbstractMonitoringAspect
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricOrigin
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricPath
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.MonitoringServiceAdapter

/**
 * Monitoring aspect for REST-endpoints.
 * Aspect performs attempt to infer caller name from invocation parameters.
 * Method should be annotated [RestMonitored] to be monitored.
 */
@Aspect
class RestMonitoringAspect(
    localTimeProvider: LocalTimeProvider,
    monitoringServiceAdapter: MonitoringServiceAdapter,
    private val callerNameExtractorChain: CallerNameExtractor,
) : AbstractMonitoringAspect<RestMonitored>(
    monitoringServiceAdapter,
    localTimeProvider
) {

    @Around("@annotation(config)")
    override fun advice(joinPoint: ProceedingJoinPoint, config: RestMonitored): Any? {
        val metricPath = listOf(
            CALLER_ALL,
            joinPoint.args.firstNotNullOfOrNull { callerNameExtractorChain.extract(it) } ?: CALLER_UNKNOWN
        )
            .map { MetricPath(
                origin = MetricOrigin.INBOUND,
                service = config.service,
                caller = it
            ) }
        return executionWrapper(joinPoint, metricPath)
    }

    internal companion object {
        private const val CALLER_UNKNOWN = "UNKNOWN"
        private const val CALLER_ALL = "ALL"
    }
}
