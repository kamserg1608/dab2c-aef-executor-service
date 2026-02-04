package ru.sbrf.dab2c.executor.library.monitoring.service.api

/**
 * Service for monitoring application metrics.
 */
interface MonitoringServiceFactory {

    /**
     * Create counter metric.
     */
    fun createCounter(name: Metric, platform: String, channel: String, tagsMap: Map<String, String>): CounterMetric

    /**
     * Create timer metric.
     */
    fun createTimer(name: Metric, platform: String, channel: String, tagsMap: Map<String, String>): RecordMetric

    /**
     * Create gauge metric.
     */
    fun createGauge(name: Metric, platform: String, channel: String, tagsMap: Map<String, String>): GaugeMetric

    fun createTimerSample(
        name: Metric,
        platform: String,
        channel: String,
        tagsMap: Map<String, String>
    ): TimerSampleMetric
}
