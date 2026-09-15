package ru.sbrf.dab2c.executor.voice.monitoring

import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric
import ru.sbrf.dab2c.executor.voice.model.VoiceSession
import ru.sbrf.dab2c.executor.voice.session.observer.VoiceSessionObserver
import ru.sbrf.dab2c.executor.voice.session.observer.VoiceSettings

/**
 * Measures the duration between the settings received from "smartIVR"
 * ([VoiceSessionObserver.onOriginalSettingsReceived]) and the settings we computed
 * ([VoiceSessionObserver.onSettingsReceived]). Records the result to
 * [ExecutorVoiceMetric.GRPC_SETTINGS_INITIALIZATION_DURATION_SECONDS].
 *
 * Timestamp of the original settings event is stored in
 * [VoiceSession.originalSettingsReceivedTimestamp] — one instance is created per session.
 */
class MonitoringSettingsReceived(
    private val metricFactory: MetricFactory,
    private val session: VoiceSession,
) : VoiceSessionObserver {

    override suspend fun onOriginalSettingsReceived(settings: VoiceSettings) {
        session.originalSettingsReceivedTimestamp = System.nanoTime()
    }

    override suspend fun onSettingsReceived(settings: VoiceSettings) {
        val timestamp = session.originalSettingsReceivedTimestamp ?: return
        recordDuration(timestamp)
    }

    private suspend fun recordDuration(startTimestamp: Long) {
        metricFactory.recordDuration(
            metric = ExecutorVoiceMetric.GRPC_SETTINGS_INITIALIZATION_DURATION_SECONDS,
            durationNs = System.nanoTime() - startTimestamp,
            tags = emptyMap()
        )
    }
}
