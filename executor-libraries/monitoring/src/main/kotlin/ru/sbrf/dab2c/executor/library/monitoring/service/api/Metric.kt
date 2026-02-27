package ru.sbrf.dab2c.executor.library.monitoring.service.api

/**
 * Represents a metric that can be collected and reported by the monitoring service.
 */
interface Metric {
    val metricName: String
}
