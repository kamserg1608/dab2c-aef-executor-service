package ru.sbrf.dab2c.executor.library.monitoring.service.impl

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.library.monitoring.service.api.CounterMetric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.GaugeMetric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.Metric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MonitoringServiceFactory
import ru.sbrf.dab2c.executor.library.monitoring.service.api.RecordMetric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.TimerSampleMetric
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * Implementation of the MonitoringService interface.
 */
@Service
class MonitoringServiceFactoryImpl(
    private val meterRegistry: MeterRegistry
) : MonitoringServiceFactory {

    override fun createCounter(
        name: Metric,
        platform: String,
        channel: String,
        tagsMap: Map<String, String>
    ): CounterMetric {
        val counter = Counter.builder(name.metricName)
            .tag(PLATFORM, platform)
            .tag(CHANNEL, channel)
            .tags(tagsMap.map { Tag.of(it.key, it.value) })
            .register(meterRegistry)

        return CounterMetric { counter.increment() }
    }

    override fun createTimer(
        name: Metric,
        platform: String,
        channel: String,
        tagsMap: Map<String, String>
    ): RecordMetric {
        val timer = Timer.builder(name.metricName)
            .tag(PLATFORM, platform)
            .tag(CHANNEL, channel)
            .tags(tagsMap.map { Tag.of(it.key, it.value) })
            .serviceLevelObjectives(
                Duration.ofMillis(FAST_RESPONSE_THRESHOLD_MS),
                Duration.ofMillis(STANDARD_RESPONSE_THRESHOLD_MS),
                Duration.ofMillis(SLOW_RESPONSE_THRESHOLD_MS)
            )
            .register(meterRegistry)

        return RecordMetric { block ->
            val startTime = System.nanoTime()
            try {
                block()
            } finally {
                val durationNs = System.nanoTime() - startTime
                timer.record(durationNs, java.util.concurrent.TimeUnit.NANOSECONDS)
            }
        }
    }

    override fun createGauge(
        name: Metric,
        platform: String,
        channel: String,
        tagsMap: Map<String, String>
    ): GaugeMetric {
        val valueHolder = AtomicInteger(0)

        val gauge = Gauge.builder(name.metricName, valueHolder) { it.get().toDouble() }
            .tag(PLATFORM, platform)
            .tag(CHANNEL, channel)
            .tags(tagsMap.map { Tag.of(it.key, it.value) })
            .register(meterRegistry)

        return GaugeMetric { gauge.value() }
    }

    override fun createTimerSample(
        name: Metric,
        platform: String,
        channel: String,
        tagsMap: Map<String, String>
    ): TimerSampleMetric {
        val timer = Timer.builder(name.metricName)
            .tag(PLATFORM, platform)
            .tag(CHANNEL, channel)
            .tags(tagsMap.map { Tag.of(it.key, it.value) })
            .serviceLevelObjectives(
                Duration.ofMillis(FAST_RESPONSE_THRESHOLD_MS),
                Duration.ofMillis(STANDARD_RESPONSE_THRESHOLD_MS),
                Duration.ofMillis(SLOW_RESPONSE_THRESHOLD_MS)
            )
            .register(meterRegistry)

        return TimerSampleMetric {
            val sample = Timer.start(meterRegistry)
            object : TimerSampleMetric.TimerSample {
                override fun stop() {
                    sample.stop(timer)
                }
            }
        }
    }

    /**
     * Companion object containing constant values used in the monitoring service.
     */
    companion object {
        private const val PLATFORM = "platform"
        private const val CHANNEL = "channel"

        private const val FAST_RESPONSE_THRESHOLD_MS = 200L
        private const val STANDARD_RESPONSE_THRESHOLD_MS = 2000L
        private const val SLOW_RESPONSE_THRESHOLD_MS = 20000L
    }
}
