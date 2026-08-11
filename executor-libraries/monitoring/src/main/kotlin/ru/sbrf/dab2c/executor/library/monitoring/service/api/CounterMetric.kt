package ru.sbrf.dab2c.executor.library.monitoring.service.api

/**
 * Counter metric.
 */
fun interface CounterMetric {

    /**
     * Increment counter by given amount.
     */
    fun increment(amount: Double)

    /**
     * Increment counter by 1.
     */
    fun increment() = increment(1.0)

    /**
     * Increment counter.
     */
    operator fun invoke() = increment()
}
