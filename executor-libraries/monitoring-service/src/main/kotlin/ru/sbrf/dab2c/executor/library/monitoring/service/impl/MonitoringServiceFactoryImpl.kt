package ru.sbrf.dab2c.executor.library.monitoring.service.impl

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.library.monitoring.service.api.CounterMetric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.Metric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MonitoringServiceFactory
import ru.sbrf.dab2c.executor.library.monitoring.service.api.RecordMetric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.TimerSampleMetric
import java.time.Duration

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

        return CounterMetric { amount -> counter.increment(amount) }
    }

    override fun createTimer(
        name: Metric,
        platform: String,
        channel: String,
        tagsMap: Map<String, String>
    ): RecordMetric {
        val timer = buildTimer(name, platform, channel, tagsMap)

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

    @Suppress("LongParameterList")
    override fun <T : Any> createGauge(
        name: Metric,
        platform: String,
        channel: String,
        tagsMap: Map<String, String>,
        stateObject: T,
        valueFunction: (T) -> Double
    ) {
        Gauge.builder(name.metricName, stateObject) { valueFunction(it) }
            .tag(PLATFORM, platform)
            .tag(CHANNEL, channel)
            .tags(tagsMap.map { Tag.of(it.key, it.value) })
            .register(meterRegistry)
    }

    override fun createTimerSample(
        name: Metric,
        platform: String,
        channel: String,
        tagsMap: Map<String, String>
    ): TimerSampleMetric {
        val timer = buildTimer(name, platform, channel, tagsMap)

        return TimerSampleMetric {
            val sample = Timer.start(meterRegistry)
            object : TimerSampleMetric.TimerSample {
                override fun stop() {
                    sample.stop(timer)
                }
            }
        }
    }

    private fun buildTimer(
        name: Metric,
        platform: String,
        channel: String,
        tagsMap: Map<String, String>
    ): Timer = Timer.builder(name.metricName)
        .tag(PLATFORM, platform)
        .tag(CHANNEL, channel)
        .tags(tagsMap.map { Tag.of(it.key, it.value) })
        .serviceLevelObjectives(
            Duration.ofMillis(FAST_RESPONSE_THRESHOLD_MS),
            Duration.ofMillis(STANDARD_RESPONSE_THRESHOLD_MS),
            Duration.ofMillis(SLOW_RESPONSE_THRESHOLD_MS)
        )
        .register(meterRegistry)

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
