package ru.sbrf.dab2c.executor.library.monitoring.service.api

/**
 * RecordMetric - Interface for using the timer metric.
 */
interface RecordMetric {
    /**
     * Recorded the working time of a code block.
     */
    suspend fun <T> record(block: suspend () -> T): T

    /**
     * Allows to call timer as function.
     */
    suspend operator fun <T> invoke(block: suspend () -> T): T = record(block)
}
