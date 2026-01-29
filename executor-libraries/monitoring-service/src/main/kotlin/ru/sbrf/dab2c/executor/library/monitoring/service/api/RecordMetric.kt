package ru.sbrf.dab2c.executor.library.monitoring.service.api

/**
 * RecordMetric - Interface for using the timer metric.
 */
fun interface RecordMetric {
    /**
     * Recorded the working time of a code block.
     */
    suspend fun record(block: suspend () -> Unit)

    /**
     * Allows to call timer as function.
     */
    suspend operator fun invoke(block: suspend () -> Unit) = record(block)
}
