package ru.sbrf.dab2c.executor.library.monitoring.service.api

/**
 * Context-aware factory for creating and recording application metrics.
 * Platform and channel tags are auto-extracted from the coroutine context.
 */
interface MetricFactory {

    /** Increment a counter by 1. */
    suspend fun incrementCounter(metric: Metric, tags: Map<String, String> = emptyMap())

    /** Increment a counter by the specified amount. */
    suspend fun incrementCounter(metric: Metric, amount: Double, tags: Map<String, String> = emptyMap())

    /** Create a timer that records duration of a suspend block. */
    suspend fun createTimer(metric: Metric, tags: Map<String, String> = emptyMap()): RecordMetric

    /** Create a timer sample for measuring long-running operations with separate start/stop. */
    suspend fun createTimerSample(metric: Metric, tags: Map<String, String> = emptyMap()): TimerSampleMetric

    /** Create a gauge that observes the given state object. */
    suspend fun <T : Any> createGauge(
        metric: Metric,
        tags: Map<String, String> = emptyMap(),
        stateObject: T,
        valueFunction: (T) -> Double
    )

    /** Record a pre-measured duration on a timer. */
    suspend fun recordDuration(metric: Metric, durationNs: Long, tags: Map<String, String> = emptyMap())

    /** Create a timer and record the given block in one call. */
    suspend fun <T> recordTimer(metric: Metric, tags: Map<String, String> = emptyMap(), block: suspend () -> T): T
}
