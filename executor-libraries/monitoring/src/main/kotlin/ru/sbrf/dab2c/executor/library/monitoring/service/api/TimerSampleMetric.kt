package ru.sbrf.dab2c.executor.library.monitoring.service.api

/**
 * A metric for measuring duration of long-running operations (e.g. gRPC streams).
 * Allows starting and stopping timer at different points in time.
 */
fun interface TimerSampleMetric {

    /**
     * Start the timer.
     */
    fun start(): TimerSample

    /**
     * Handle to stop the timer and record the duration.
     */
    interface TimerSample {

        /**
         *  Stop the timer.
         */
        fun stop()
    }
}
