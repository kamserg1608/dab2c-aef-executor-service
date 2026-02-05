package ru.sbrf.dab2c.executor.library.monitoring.service.api

/**
 * Interface for gauge metrics.
 */
fun interface GaugeMetric {

    /**
     * Sets the value of the gauge metric.
     */
    fun set(value: Double)
}
