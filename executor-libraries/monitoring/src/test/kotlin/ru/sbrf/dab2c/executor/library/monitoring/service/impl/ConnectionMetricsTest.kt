package ru.sbrf.dab2c.executor.library.monitoring.service.impl

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.HeadersElement
import ru.sbrf.dab2c.executor.library.monitoring.service.api.ConnectionMetrics
import ru.sbrf.dab2c.executor.library.monitoring.service.api.Metric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import ru.sbrf.dab2c.executor.library.monitoring.service.api.TimerSampleMetric

class ConnectionMetricsTest {

    private lateinit var metricFactory: MetricFactory
    private lateinit var connectionMetrics: ConnectionMetrics
    private lateinit var mockTimerSample: TimerSampleMetric.TimerSample

    private val activeMetric = TestMetric("connections_active")
    private val totalMetric = TestMetric("connections_total")
    private val durationMetric = TestMetric("connections_duration")

    private val testHeaders = Headers(
        mapOf("x-platform" to "test-platform", "x-channel" to "test-channel")
    )

    @BeforeEach
    fun setUp() {
        metricFactory = mockk(relaxed = true)
        mockTimerSample = mockk(relaxed = true)
        val mockTimerSampleMetric = mockk<TimerSampleMetric>()
        coEvery { metricFactory.createTimerSample(durationMetric, any()) } returns mockTimerSampleMetric
        every { mockTimerSampleMetric.start() } returns mockTimerSample

        connectionMetrics = ConnectionMetrics(metricFactory, activeMetric, totalMetric, durationMetric)
    }

    @Test
    fun `openConnection should create gauge, increment counter, and start timer`() = runTest {
        val handle = withTestContext { connectionMetrics.openConnection() }

        assertThat(handle.connectionKey).isEqualTo("test-platform:test-channel")
        assertThat(handle.activeCount).isEqualTo(1)
        coVerify { metricFactory.createGauge(activeMetric, emptyMap(), any(), any<(Any) -> Double>()) }
        coVerify { metricFactory.incrementCounter(totalMetric, emptyMap()) }
        coVerify { metricFactory.createTimerSample(durationMetric, emptyMap()) }
    }

    @Test
    fun `close should decrement active counter and stop timer`() = runTest {
        val handle = withTestContext { connectionMetrics.openConnection() }
        assertThat(handle.activeCount).isEqualTo(1)

        handle.close()

        assertThat(handle.activeCount).isEqualTo(0)
        verify { mockTimerSample.stop() }
    }

    @Test
    fun `multiple connections with same key should share active counter`() = runTest {
        val handle1 = withTestContext { connectionMetrics.openConnection() }
        val handle2 = withTestContext { connectionMetrics.openConnection() }

        assertThat(handle1.activeCount).isEqualTo(2)
        assertThat(handle2.activeCount).isEqualTo(2)

        handle1.close()
        assertThat(handle2.activeCount).isEqualTo(1)

        handle2.close()
        assertThat(handle2.activeCount).isEqualTo(0)
    }

    private suspend fun <T> withTestContext(block: suspend () -> T): T =
        withContext(HeadersElement(testHeaders)) { block() }

    private data class TestMetric(override val metricName: String) : Metric
}
