package ru.sbrf.dab2c.executor.library.monitoring.service.impl

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.library.monitoring.service.api.CounterMetric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.Metric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricTags
import ru.sbrf.dab2c.executor.library.monitoring.service.api.RecordMetric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.TimerSampleMetric
import java.time.Duration

/** Micrometer-backed implementation of [MetricFactory]. */
@Service
class MetricFactoryImpl(
    private val meterRegistry: MeterRegistry
) : MetricFactory {

    override suspend fun incrementCounter(metric: Metric, tags: Map<String, String>) {
        buildCounter(metric, tags).increment()
    }

    override suspend fun incrementCounter(metric: Metric, amount: Double, tags: Map<String, String>) {
        buildCounter(metric, tags).increment(amount)
    }

    override suspend fun createTimer(metric: Metric, tags: Map<String, String>): RecordMetric {
        val timer = buildTimer(metric, tags)
        return object : RecordMetric {
            override suspend fun <T> record(block: suspend () -> T): T {
                val startTime = System.nanoTime()
                try {
                    return block()
                } finally {
                    val durationNs = System.nanoTime() - startTime
                    timer.record(durationNs, java.util.concurrent.TimeUnit.NANOSECONDS)
                }
            }
        }
    }

    override suspend fun createTimerSample(metric: Metric, tags: Map<String, String>): TimerSampleMetric {
        val timer = buildTimer(metric, tags)
        return TimerSampleMetric {
            val sample = Timer.start(meterRegistry)
            object : TimerSampleMetric.TimerSample {
                override fun stop() {
                    sample.stop(timer)
                }
            }
        }
    }

    override suspend fun <T : Any> createGauge(
        metric: Metric,
        tags: Map<String, String>,
        stateObject: T,
        valueFunction: (T) -> Double
    ) {
        val (platform, channel) = currentPlatformAndChannel()
        Gauge.builder(metric.metricName, stateObject) { valueFunction(it) }
            .tag(MetricTags.PLATFORM, platform)
            .tag(MetricTags.CHANNEL, channel)
            .tags(tags.map { Tag.of(it.key, it.value) })
            .register(meterRegistry)
    }

    override suspend fun recordDuration(metric: Metric, durationNs: Long, tags: Map<String, String>) {
        buildTimer(metric, tags).record(durationNs, java.util.concurrent.TimeUnit.NANOSECONDS)
    }

    override suspend fun <T> recordTimer(metric: Metric, tags: Map<String, String>, block: suspend () -> T): T =
        createTimer(metric, tags).record(block)

    private suspend fun buildCounter(metric: Metric, tags: Map<String, String>): CounterMetric {
        val (platform, channel) = currentPlatformAndChannel()
        val counter = Counter.builder(metric.metricName)
            .tag(MetricTags.PLATFORM, platform)
            .tag(MetricTags.CHANNEL, channel)
            .tags(tags.map { Tag.of(it.key, it.value) })
            .register(meterRegistry)
        return CounterMetric { amount -> counter.increment(amount) }
    }

    private suspend fun buildTimer(metric: Metric, tags: Map<String, String>): Timer {
        val (platform, channel) = currentPlatformAndChannel()
        return Timer.builder(metric.metricName)
            .tag(MetricTags.PLATFORM, platform)
            .tag(MetricTags.CHANNEL, channel)
            .tags(tags.map { Tag.of(it.key, it.value) })
            .serviceLevelObjectives(
                Duration.ofMillis(FAST_RESPONSE_THRESHOLD_MS),
                Duration.ofMillis(STANDARD_RESPONSE_THRESHOLD_MS),
                Duration.ofMillis(SLOW_RESPONSE_THRESHOLD_MS)
            )
            .register(meterRegistry)
    }

    private suspend fun currentPlatformAndChannel(): Pair<String, String> {
        val headers = currentHeaders()
        return headers.getHeader(RequestHeader.PLATFORM) to headers.getHeader(RequestHeader.CHANNEL)
    }

    /** Timer SLO threshold constants. */
    companion object {
        private const val FAST_RESPONSE_THRESHOLD_MS = 200L
        private const val STANDARD_RESPONSE_THRESHOLD_MS = 2000L
        private const val SLOW_RESPONSE_THRESHOLD_MS = 20000L
    }
}
