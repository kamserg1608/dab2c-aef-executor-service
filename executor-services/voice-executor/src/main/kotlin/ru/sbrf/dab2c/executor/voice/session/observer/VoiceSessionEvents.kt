package ru.sbrf.dab2c.executor.voice.session.observer

/** Text spoken in a single replica with the timestamps of the window that produced it (millis since epoch). */
data class Replica(
    val text: String,
    val startedAtMs: Long,
    val endedAtMs: Long,
)

/** Event observed within a single dialog turn. */
sealed interface TurnEvent {
    val atMs: Long
}

/** Model requested a function call. */
data class FunctionCallReceived(
    val name: String,
    val arguments: String,
    override val atMs: Long,
) : TurnEvent

/** Executor sent a function result back to the model. */
data class FunctionResultSent(
    val name: String,
    val content: String,
    override val atMs: Long,
) : TurnEvent

/** Non-fatal warning emitted by the downstream voice service. */
data class WarningEmitted(
    val message: String,
    override val atMs: Long,
) : TurnEvent

/** Error emitted by the downstream voice service. */
data class ErrorEmitted(
    val status: Int,
    val message: String,
    override val atMs: Long,
) : TurnEvent

/**
 * One input → assistant reply pair. A `null` replica means that side did not speak in this turn
 * (e.g. bot greets first, or the session ended before the model answered).
 */
data class TurnCompleted(
    val userReplica: Replica?,
    val assistantReplica: Replica?,
    val turnEvents: List<TurnEvent>,
    val totalTokens: Int?,
    val llmUsage: LlmUsage? = null,
)

/** LLM usage reported by the downstream voice service within one dialog turn. */
data class LlmUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val precachedPromptTokens: Int,
    val model: String?,
    val finishReason: String?,
)

/** Voice settings observed at session bootstrap. */
data class VoiceSettings(
    val settingsData: Map<String, Any?>,
)
