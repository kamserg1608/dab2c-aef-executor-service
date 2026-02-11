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
     * Create gauge metric that observes the given state object.
     */
    @Suppress("LongParameterList")
    fun <T : Any> createGauge(
        name: Metric,
        platform: String,
        channel: String,
        tagsMap: Map<String, String>,
        stateObject: T,
        valueFunction: (T) -> Double
    )

    /**
     * Create timer sample metric.
     */
    fun createTimerSample(
        name: Metric,
        platform: String,
        channel: String,
        tagsMap: Map<String, String>
    ): TimerSampleMetric
}
