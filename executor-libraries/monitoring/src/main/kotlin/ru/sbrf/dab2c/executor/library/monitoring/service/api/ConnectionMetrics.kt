package ru.sbrf.dab2c.executor.library.monitoring.service.api

import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Tracks connection lifecycle metrics: active gauge, total counter, and duration timer. */
class ConnectionMetrics(
    private val metricFactory: MetricFactory,
    private val activeMetric: Metric,
    private val totalMetric: Metric,
    private val durationMetric: Metric
) {
    private val activeConnectionCounters = ConcurrentHashMap<String, AtomicInteger>()

    /** Open a new connection, registering gauge/counter/timer metrics. */
    suspend fun openConnection(tags: Map<String, String> = emptyMap()): ConnectionHandle {
        val headers = currentHeaders()
        val platform = headers.getHeader(RequestHeader.PLATFORM)
        val channel = headers.getHeader(RequestHeader.CHANNEL)
        val connectionKey = "$platform:$channel"

        val counter = activeConnectionCounters
            .computeIfAbsent(connectionKey) { AtomicInteger(0) }

        metricFactory.createGauge(activeMetric, tags, counter) { it.get().toDouble() }
        counter.incrementAndGet()

        metricFactory.incrementCounter(totalMetric, tags)

        val timerSample = metricFactory.createTimerSample(durationMetric, tags).start()

        return ConnectionHandle(connectionKey, counter, timerSample)
    }
}

/** Handle for an open connection; call [close] when the connection ends. */
class ConnectionHandle(
    val connectionKey: String,
    private val counter: AtomicInteger,
    private val timerSample: TimerSampleMetric.TimerSample
) {
    val activeCount: Int get() = counter.get()

    /** Decrement active counter and stop the duration timer. */
    fun close() {
        counter.decrementAndGet()
        timerSample.stop()
    }
}
