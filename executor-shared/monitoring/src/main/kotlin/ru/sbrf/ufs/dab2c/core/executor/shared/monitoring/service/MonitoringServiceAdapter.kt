package ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service

import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricPath

/**
 * Abstraction over [CoreMonitoringService] and defines the contract for reporting metrics.
 */
interface MonitoringServiceAdapter {

    /**
     * Reports a [metricPath] with given [value] to the monitoring system.
     */
    fun reportMetric(metricPath: MetricPath, value: Double = 1.0)
}
