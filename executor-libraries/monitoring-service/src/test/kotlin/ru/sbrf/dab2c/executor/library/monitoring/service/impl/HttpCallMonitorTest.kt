package ru.sbrf.dab2c.executor.library.monitoring.service.impl

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.HeadersElement
import ru.sbrf.dab2c.executor.library.monitoring.service.api.HttpCallDescriptor
import ru.sbrf.dab2c.executor.library.monitoring.service.api.Metric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricTags
import ru.sbrf.dab2c.executor.library.monitoring.service.api.monitorHttpCall
import java.io.IOException
import java.util.concurrent.TimeoutException

class HttpCallMonitorTest {

    private lateinit var meterRegistry: SimpleMeterRegistry
    private lateinit var metricFactory: MetricFactoryImpl

    private val timerMetric = TestMetric("http_duration")
    private val counterMetric = TestMetric("http_total")

    private val testHeaders = Headers(
        mapOf("x-platform" to "test-platform", "x-channel" to "test-channel")
    )

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()
        metricFactory = MetricFactoryImpl(meterRegistry)
    }

    @Test
    fun `monitorHttpCall should record timer and increment counter with assembled tags`() = runTest {
        val descriptor = HttpCallDescriptor(
            destinationService = "test-service",
            endpoint = "/api/test"
        )

        val result = withTestContext {
            metricFactory.monitorHttpCall(timerMetric, counterMetric, descriptor) { "ok" }
        }

        assertThat(result).isEqualTo("ok")

        val timer = meterRegistry.find("http_duration").timer()
        assertThat(timer).isNotNull
        assertThat(timer!!.count()).isEqualTo(1)
        assertThat(timer.id.getTag(MetricTags.DESTINATION_SERVICE)).isEqualTo("test-service")
        assertThat(timer.id.getTag(MetricTags.ENDPOINT)).isEqualTo("/api/test")
        assertThat(timer.id.getTag(MetricTags.METHOD)).isEqualTo("post")
        assertThat(timer.id.getTag(MetricTags.PLATFORM)).isEqualTo("test-platform")
        assertThat(timer.id.getTag(MetricTags.CHANNEL)).isEqualTo("test-channel")
        assertThat(timer.id.getTag(MetricTags.STATUS_CODE)).isEqualTo("200")

        val counter = meterRegistry.find("http_total").counter()
        assertThat(counter).isNotNull
        assertThat(counter!!.count()).isEqualTo(1.0)
        assertThat(counter.id.getTag(MetricTags.STATUS_CODE)).isEqualTo("200")
        assertThat(counter.id.getTag(MetricTags.DESTINATION_SERVICE)).isEqualTo("test-service")
        assertThat(counter.id.getTag(MetricTags.PLATFORM)).isEqualTo("test-platform")
    }

    @Test
    fun `monitorHttpCall should use default method post`() = runTest {
        val descriptor = HttpCallDescriptor(destinationService = "svc", endpoint = "/ep")

        withTestContext {
            metricFactory.monitorHttpCall(timerMetric, counterMetric, descriptor) { }
        }

        val timer = meterRegistry.find("http_duration").timer()
        assertThat(timer!!.id.getTag(MetricTags.METHOD)).isEqualTo("post")
    }

    @Test
    fun `monitorHttpCall should record timer and counter with timeout status on TimeoutException`() = runTest {
        val descriptor = HttpCallDescriptor(destinationService = "svc", endpoint = "/ep")

        runCatching {
            withTestContext {
                metricFactory.monitorHttpCall(timerMetric, counterMetric, descriptor) {
                    throw TimeoutException("timed out")
                }
            }
        }

        val timer = meterRegistry.find("http_duration").timer()
        assertThat(timer).isNotNull
        assertThat(timer!!.count()).isEqualTo(1)
        assertThat(timer.id.getTag(MetricTags.STATUS_CODE)).isEqualTo("timeout")

        val counter = meterRegistry.find("http_total").counter()
        assertThat(counter).isNotNull
        assertThat(counter!!.count()).isEqualTo(1.0)
        assertThat(counter.id.getTag(MetricTags.STATUS_CODE)).isEqualTo("timeout")
    }

    @Test
    fun `monitorHttpCall should record timer and counter with network_error status on IOException`() = runTest {
        val descriptor = HttpCallDescriptor(destinationService = "svc", endpoint = "/ep")

        runCatching {
            withTestContext {
                metricFactory.monitorHttpCall(timerMetric, counterMetric, descriptor) {
                    throw IOException("connection refused")
                }
            }
        }

        val timer = meterRegistry.find("http_duration").timer()
        assertThat(timer).isNotNull
        assertThat(timer!!.id.getTag(MetricTags.STATUS_CODE)).isEqualTo("network_error")

        val counter = meterRegistry.find("http_total").counter()
        assertThat(counter).isNotNull
        assertThat(counter!!.id.getTag(MetricTags.STATUS_CODE)).isEqualTo("network_error")
    }

    @Test
    fun `monitorHttpCall should record timer and counter with exception status on RuntimeException`() = runTest {
        val descriptor = HttpCallDescriptor(destinationService = "svc", endpoint = "/ep")

        runCatching {
            withTestContext {
                metricFactory.monitorHttpCall(timerMetric, counterMetric, descriptor) {
                    throw RuntimeException("something broke")
                }
            }
        }

        val timer = meterRegistry.find("http_duration").timer()
        assertThat(timer).isNotNull
        assertThat(timer!!.id.getTag(MetricTags.STATUS_CODE)).isEqualTo("exception")

        val counter = meterRegistry.find("http_total").counter()
        assertThat(counter).isNotNull
        assertThat(counter!!.id.getTag(MetricTags.STATUS_CODE)).isEqualTo("exception")
    }

    @Test
    fun `monitorHttpCall should rethrow the original exception`() = runTest {
        val descriptor = HttpCallDescriptor(destinationService = "svc", endpoint = "/ep")

        val thrown = runCatching {
            withTestContext {
                metricFactory.monitorHttpCall(timerMetric, counterMetric, descriptor) {
                    throw IllegalStateException("original error")
                }
            }
        }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
        assertThat(thrown).hasMessage("original error")
    }

    private suspend fun <T> withTestContext(block: suspend () -> T): T =
        withContext(HeadersElement(testHeaders)) { block() }

    private data class TestMetric(override val metricName: String) : Metric
}
