package ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.aspect

import org.aspectj.lang.ProceedingJoinPoint
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricPath
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricType
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.MonitoringServiceAdapter

/**
 * Abstract monitoring aspect.
 */
@Suppress("TooGenericExceptionCaught")
abstract class AbstractMonitoringAspect<T>(
    private val monitoringServiceAdapter: MonitoringServiceAdapter,
    private val localTimeProvider: LocalTimeProvider
) {

    /**
     * Advice method should collect all necessary information from [config] invocation parameters into List<MetricPath>
     * and then invoke [executionWrapper].
     */
    abstract fun advice(joinPoint: ProceedingJoinPoint, config: T): Any?

    /**
     * Wraps [joinPoint] invocation and reports SUCCESS & SUCCESS_TIME
     * or ERROR & ERROR_TIME metrics for provided [metricPath]. [metricPath] can be various size.
     * Method will public [metricPath.size] sets of SUCCESS & SUCCESS_TIME or ERROR & ERROR_TIME metrics.
     */
    protected fun executionWrapper(joinPoint: ProceedingJoinPoint, metricPath: List<MetricPath>): Any? {

        val startTime = localTimeProvider.currentTimeMillis()

        try {
            val result = joinPoint.proceed()
            val executionTime = localTimeProvider.currentTimeMillis() - startTime
            reportMetric(metricPath, MetricType.SUCCESS)
            reportMetric(metricPath, MetricType.SUCCESS_TIME, executionTime)
            return result
        } catch (e: Exception) {
            val executionTime = localTimeProvider.currentTimeMillis() - startTime
            reportMetric(metricPath, MetricType.ERROR)
            reportMetric(metricPath, MetricType.ERROR_TIME, executionTime)
            throw e
        }
    }

    private fun reportMetric(path: List<MetricPath>, type: MetricType, executionTime: Long = 1) {
        path.forEach { monitoringServiceAdapter.reportMetric(it.copy(metricType = type), executionTime.toDouble()) }
    }
}
