package ru.sbrf.dab2c.executor.library.monitoring.service.api

/**
 * Counter metric.
 */
fun interface CounterMetric {

    /**
     * Increment counter.
     */
    fun increment()

    /**
     * Increment counter.
     */
    operator fun invoke() = increment()
}
