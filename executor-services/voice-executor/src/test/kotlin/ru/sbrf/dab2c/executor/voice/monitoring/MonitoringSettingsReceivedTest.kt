package ru.sbrf.dab2c.executor.voice.monitoring

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric
import ru.sbrf.dab2c.executor.voice.model.VoiceSession
import ru.sbrf.dab2c.executor.voice.session.observer.VoiceSettings

/**
 * Tests for [MonitoringSettingsReceived].
 */
class MonitoringSettingsReceivedTest {

    private val metricFactory: MetricFactory = mockk(relaxed = true)
    private val session = VoiceSession()
    private val observer = MonitoringSettingsReceived(metricFactory, session)

    private val originalSettings = VoiceSettings(mapOf("version" to "1.0"))
    private val settings = VoiceSettings(mapOf("version" to "1.0", "model" to "giga"))

    @Test
    fun `should record duration when both events are received`() = runTest {
        coEvery {
            metricFactory.recordDuration(
                metric = ExecutorVoiceMetric.GRPC_SETTINGS_INITIALIZATION_DURATION_SECONDS,
                durationNs = any<Long>(),
                tags = any()
            )
        } just runs

        observer.onOriginalSettingsReceived(originalSettings)
        observer.onSettingsReceived(settings)

        assertThat(session.originalSettingsReceivedTimestamp).isNotNull
        coVerify(exactly = 1) {
            metricFactory.recordDuration(
                metric = ExecutorVoiceMetric.GRPC_SETTINGS_INITIALIZATION_DURATION_SECONDS,
                durationNs = any<Long>(),
                tags = any()
            )
        }
    }

    @Test
    fun `should not record when only original settings received`() = runTest {
        observer.onOriginalSettingsReceived(originalSettings)

        assertThat(session.originalSettingsReceivedTimestamp).isNotNull
        coVerify(exactly = 0) {
            metricFactory.recordDuration(any(), any<Long>(), any())
        }
    }

    @Test
    fun `should not record when only settings received without original`() = runTest {
        assertThat(session.originalSettingsReceivedTimestamp).isNull()

        observer.onSettingsReceived(settings)

        coVerify(exactly = 0) {
            metricFactory.recordDuration(any(), any<Long>(), any())
        }
    }

    @Test
    fun `should record each session independently`() = runTest {
        coEvery {
            metricFactory.recordDuration(
                metric = ExecutorVoiceMetric.GRPC_SETTINGS_INITIALIZATION_DURATION_SECONDS,
                durationNs = any<Long>(),
                tags = any()
            )
        } just runs

        observer.onOriginalSettingsReceived(originalSettings)
        observer.onSettingsReceived(settings)
        coVerify(exactly = 1) {
            metricFactory.recordDuration(
                metric = ExecutorVoiceMetric.GRPC_SETTINGS_INITIALIZATION_DURATION_SECONDS,
                durationNs = any<Long>(),
                tags = any()
            )
        }

        val newObserver = MonitoringSettingsReceived(metricFactory, session)
        val newOriginal = VoiceSettings(mapOf("version" to "2.0"))
        val newSettings = VoiceSettings(mapOf("version" to "2.0", "lang" to "ru"))

        newObserver.onOriginalSettingsReceived(newOriginal)
        newObserver.onSettingsReceived(newSettings)
        coVerify(exactly = 2) {
            metricFactory.recordDuration(
                metric = ExecutorVoiceMetric.GRPC_SETTINGS_INITIALIZATION_DURATION_SECONDS,
                durationNs = any<Long>(),
                tags = any()
            )
        }
    }
}
