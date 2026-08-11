package ru.sbrf.dab2c.executor.library.monitoring.service.impl

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.HeadersElement
import ru.sbrf.dab2c.executor.library.monitoring.service.api.Metric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricTags

class MetricFactoryImplTest {

    private lateinit var meterRegistry: SimpleMeterRegistry
    private lateinit var metricFactory: MetricFactoryImpl

    private val testHeaders = Headers(
        mapOf("x-platform" to "test-platform", "x-channel" to "test-channel")
    )

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
        metricFactory = MetricFactoryImpl(meterRegistry)
    }

    @Test
    fun `incrementCounter should register counter with auto-extracted platform and channel`() = runTest {
        withTestContext {
            metricFactory.incrementCounter(TestMetric("test_counter"), mapOf("key" to "value"))
        }

        val counter = meterRegistry.find("test_counter").counter()
        assertThat(counter).isNotNull
        assertThat(counter!!.count()).isEqualTo(1.0)
        assertThat(counter.id.getTag(MetricTags.PLATFORM)).isEqualTo("test-platform")
        assertThat(counter.id.getTag(MetricTags.CHANNEL)).isEqualTo("test-channel")
        assertThat(counter.id.getTag("key")).isEqualTo("value")
    }

    @Test
    fun `incrementCounter with amount should increment by specified value`() = runTest {
        withTestContext {
            metricFactory.incrementCounter(TestMetric("test_counter"), 5.0)
        }

        val counter = meterRegistry.find("test_counter").counter()
        assertThat(counter).isNotNull
        assertThat(counter!!.count()).isEqualTo(5.0)
    }

    @Test
    fun `createTimer should record duration of block execution`() = runTest {
        withTestContext {
            val timer = metricFactory.createTimer(TestMetric("test_timer"), mapOf("op" to "read"))
            timer.record { "result" }
        }

        val timer = meterRegistry.find("test_timer").timer()
        assertThat(timer).isNotNull
        assertThat(timer!!.count()).isEqualTo(1)
        assertThat(timer.id.getTag(MetricTags.PLATFORM)).isEqualTo("test-platform")
        assertThat(timer.id.getTag(MetricTags.CHANNEL)).isEqualTo("test-channel")
        assertThat(timer.id.getTag("op")).isEqualTo("read")
    }

    @Test
    fun `recordTimer should create and record timer in one call`() = runTest {
        val result = withTestContext {
            metricFactory.recordTimer(TestMetric("test_timer")) { "computed" }
        }

        assertThat(result).isEqualTo("computed")
        val timer = meterRegistry.find("test_timer").timer()
        assertThat(timer).isNotNull
        assertThat(timer!!.count()).isEqualTo(1)
    }

    @Test
    fun `createTimerSample should measure duration between start and stop`() = runTest {
        val sample = withTestContext {
            metricFactory.createTimerSample(TestMetric("test_sample")).start()
        }

        sample.stop()

        val timer = meterRegistry.find("test_sample").timer()
        assertThat(timer).isNotNull
        assertThat(timer!!.count()).isEqualTo(1)
        assertThat(timer.id.getTag(MetricTags.PLATFORM)).isEqualTo("test-platform")
        assertThat(timer.id.getTag(MetricTags.CHANNEL)).isEqualTo("test-channel")
    }

    @Test
    fun `recordDuration should register timer and record given duration`() = runTest {
        withTestContext {
            metricFactory.recordDuration(TestMetric("test_duration"), 500_000_000L, mapOf("op" to "call"))
        }

        val timer = meterRegistry.find("test_duration").timer()
        assertThat(timer).isNotNull
        assertThat(timer!!.count()).isEqualTo(1)
        val totalTimeMs = timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)
        assertThat(totalTimeMs).isCloseTo(500.0, org.assertj.core.data.Offset.offset(1.0))
        assertThat(timer.id.getTag(MetricTags.PLATFORM)).isEqualTo("test-platform")
        assertThat(timer.id.getTag(MetricTags.CHANNEL)).isEqualTo("test-channel")
        assertThat(timer.id.getTag("op")).isEqualTo("call")
    }

    @Test
    fun `createGauge should register gauge with state object`() = runTest {
        val stateHolder = java.util.concurrent.atomic.AtomicInteger(42)

        withTestContext {
            metricFactory.createGauge(TestMetric("test_gauge"), stateObject = stateHolder) { it.get().toDouble() }
        }

        val gauge = meterRegistry.find("test_gauge").gauge()
        assertThat(gauge).isNotNull
        assertThat(gauge!!.value()).isEqualTo(42.0)
        assertThat(gauge.id.getTag(MetricTags.PLATFORM)).isEqualTo("test-platform")
        assertThat(gauge.id.getTag(MetricTags.CHANNEL)).isEqualTo("test-channel")
    }

    private suspend fun <T> withTestContext(block: suspend () -> T): T =
        withContext(HeadersElement(testHeaders)) { block() }

    private data class TestMetric(override val metricName: String) : Metric
}
