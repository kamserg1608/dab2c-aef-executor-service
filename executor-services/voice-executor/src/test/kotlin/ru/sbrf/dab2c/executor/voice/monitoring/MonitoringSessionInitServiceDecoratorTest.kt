package ru.sbrf.dab2c.executor.voice.monitoring

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric
import ru.sbrf.dab2c.executor.voice.model.VoiceSessionFeatureToggles
import ru.sbrf.dab2c.executor.voice.service.api.SessionInitResult
import ru.sbrf.dab2c.executor.voice.service.api.SessionInitService

class MonitoringSessionInitServiceDecoratorTest {

    private val delegate: SessionInitService = mockk()
    private val metricFactory: MetricFactory = mockk(relaxed = true)

    private lateinit var decorator: MonitoringSessionInitServiceDecorator

    @BeforeEach
    fun setUp() {
        decorator = MonitoringSessionInitServiceDecorator(
            delegate = delegate,
            metricFactory = metricFactory
        )
    }

    @Test
    fun `should initialize session and record duration metrics on success`() = runTest {
        coEvery { delegate.initialize() } returns sessionInitResult()

        val result = decorator.initialize()

        assertThat(result).isNotNull()
        assertThat(result.sessionInfo).isNotNull()
        assertThat(result.featureToggles).isNotNull()

        coVerify(exactly = 1) {
            delegate.initialize()
        }
        coVerify(exactly = 1) {
            metricFactory.recordDuration(
                metric = ExecutorVoiceMetric.GRPC_SESSION_INITIALIZATION_DURATION_SECONDS,
                durationNs = any(),
                tags = emptyMap()
            )
        }

        confirmVerified(delegate, metricFactory)
    }

    @Test
    fun `should record duration metrics and rethrow on initialization error`() = runTest {
        val exception = IllegalStateException("initialization failed")
        coEvery { delegate.initialize() } throws exception

        val thrown = catchThrowable {
            decorator.initialize()
        }

        assertThat(thrown).isSameAs(exception)

        coVerify(exactly = 1) {
            delegate.initialize()
        }
        coVerify(exactly = 1) {
            metricFactory.recordDuration(
                metric = ExecutorVoiceMetric.GRPC_SESSION_INITIALIZATION_DURATION_SECONDS,
                durationNs = any(),
                tags = emptyMap()
            )
        }

        confirmVerified(delegate, metricFactory)
    }

    @Test
    fun `should rethrow cancellation and not record duration metrics`() = runTest {
        val exception = CancellationException("cancelled")
        coEvery { delegate.initialize() } throws exception

        val thrown = catchThrowable {
            decorator.initialize()
        }

        assertThat(thrown).isSameAs(exception)

        coVerify(exactly = 1) {
            delegate.initialize()
        }
        coVerify(exactly = 0) {
            metricFactory.recordDuration(
                metric = ExecutorVoiceMetric.GRPC_SESSION_INITIALIZATION_DURATION_SECONDS,
                durationNs = any(),
                tags = any()
            )
        }

        confirmVerified(delegate, metricFactory)
    }

    private suspend fun catchThrowable(block: suspend () -> Unit): Throwable? =
        try {
            block()
            null
        } catch (e: Throwable) {
            e
        }

    private fun sessionInitResult(): SessionInitResult =
        SessionInitResult(
            sessionInfo = mockk(),
            featureToggles = VoiceSessionFeatureToggles(emptyMap())
        )
}
