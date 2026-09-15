package ru.sbrf.dab2c.executor.voice.session.observer

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException

/**
 * Dispatches each event to every child sequentially. A failing child is logged at WARN and
 * skipped — remaining children still receive the event, the voice session is not affected.
 */
@Suppress("TooManyFunctions")
class CompositeVoiceSessionObserver(
    private val observers: List<VoiceSessionObserver>,
) : VoiceSessionObserver {

    override suspend fun onSessionStarted() =
        forEach("onSessionStarted") { it.onSessionStarted() }

    override suspend fun onOriginalSettingsReceived(settings: VoiceSettings) =
        forEach("onOriginalSettingsReceived") { it.onOriginalSettingsReceived(settings) }

    override suspend fun onSettingsReceived(settings: VoiceSettings) =
        forEach("onSettingsReceived") { it.onSettingsReceived(settings) }

    override suspend fun onSessionCompleted(cause: Throwable?) =
        forEach("onSessionCompleted") { it.onSessionCompleted(cause) }

    override suspend fun onUserReplicaStarted(atMs: Long) =
        forEach("onUserReplicaStarted") { it.onUserReplicaStarted(atMs) }

    override suspend fun onUserReplicaCompleted(replica: Replica) =
        forEach("onUserReplicaCompleted") { it.onUserReplicaCompleted(replica) }

    override suspend fun onAssistantReplicaStarted(atMs: Long) =
        forEach("onAssistantReplicaStarted") { it.onAssistantReplicaStarted(atMs) }

    override suspend fun onAssistantReplicaCompleted(replica: Replica) =
        forEach("onAssistantReplicaCompleted") { it.onAssistantReplicaCompleted(replica) }

    override suspend fun onAssistantInterrupted(atMs: Long) =
        forEach("onAssistantInterrupted") { it.onAssistantInterrupted(atMs) }

    override suspend fun onFunctionCallReceived(event: FunctionCallReceived) =
        forEach("onFunctionCallReceived") { it.onFunctionCallReceived(event) }

    override suspend fun onFunctionResultSent(event: FunctionResultSent) =
        forEach("onFunctionResultSent") { it.onFunctionResultSent(event) }

    override suspend fun onUsageUpdated(totalTokens: Int) =
        forEach("onUsageUpdated") { it.onUsageUpdated(totalTokens) }

    override suspend fun onWarningEmitted(event: WarningEmitted) =
        forEach("onWarningEmitted") { it.onWarningEmitted(event) }

    override suspend fun onErrorEmitted(event: ErrorEmitted) =
        forEach("onErrorEmitted") { it.onErrorEmitted(event) }

    override suspend fun onTurnCompleted(event: TurnCompleted) =
        forEach("onTurnCompleted") { it.onTurnCompleted(event) }

    private suspend fun forEach(eventName: String, block: suspend (VoiceSessionObserver) -> Unit) {
        for (observer in observers) {
            try {
                block(observer)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn(e) { "Observer ${observer::class.simpleName} failed on $eventName" }
            }
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
