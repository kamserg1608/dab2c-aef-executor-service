package ru.sbrf.dab2c.executor.voice.session.observer

/** Read-only hook into a voice session. */
@Suppress("ComplexInterface", "TooManyFunctions")
interface VoiceSessionObserver {

    /** First chunk of the session arrived. */
    suspend fun onSessionStarted() {}

    /** Fires at most once per session. */
    suspend fun onSettingsReceived(settings: VoiceSettings) {}

    /** Terminal hook. `cause == null` means normal completion. */
    suspend fun onSessionCompleted(cause: Throwable?) {}

    /** First user-audio chunk in a new user turn. */
    suspend fun onUserReplicaStarted(atMs: Long) {}

    /** All user-input transcription fragments of the current turn aggregated into a single replica. */
    suspend fun onUserReplicaCompleted(replica: Replica) {}

    /** First assistant signal of a new assistant segment. */
    suspend fun onAssistantReplicaStarted(atMs: Long) {}

    /** May fire multiple times per turn when a function-call dance produces several segments. */
    suspend fun onAssistantReplicaCompleted(replica: Replica) {}

    /** Fires in addition to `onAssistantReplicaCompleted`, not instead of it. */
    suspend fun onAssistantInterrupted(atMs: Long) {}

    /** Always emitted before the closing `onAssistantReplicaCompleted` of the same segment. */
    suspend fun onFunctionCallReceived(event: FunctionCallReceived) {}

    /** Executor sent a function result back to the model. */
    suspend fun onFunctionResultSent(event: FunctionResultSent) {}

    /** Cumulative token counter for the session. */
    suspend fun onUsageUpdated(totalTokens: Int) {}

    /** Downstream warning observed within the current turn. */
    suspend fun onWarningEmitted(event: WarningEmitted) {}

    /** Downstream error observed within the current turn. */
    suspend fun onErrorEmitted(event: ErrorEmitted) {}

    /** One input → all assistant segments → next input boundary. */
    suspend fun onTurnCompleted(event: TurnCompleted) {}
}
