package ru.sbrf.dab2c.executor.voice.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext

/**
 * Per-session lifecycle object that owns all session-scoped resources.
 * Provides a single teardown point via [close].
 */
class VoiceSession(
    val state: MutableStateFlow<ProcessingState> = MutableStateFlow(ProcessingState.AwaitingContext),
    val callbackChannels: CallbackChannels = CallbackChannels(
        downstream = Channel(capacity = Channel.BUFFERED),
        upstream = Channel(capacity = Channel.BUFFERED)
    )
) {
    private val scope = CoroutineScope(SupervisorJob())

    /**
     * Launches a session-scoped coroutine that inherits the caller's coroutine context
     * (headers, session info, etc.) while using the session's [SupervisorJob] for lifecycle.
     * Cancelled on [close].
     */
    suspend fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        val callerContext = currentCoroutineContext().minusKey(Job)
        return scope.launch(callerContext + MDCContext(), block = block)
    }

    /** Closes callback channels, unblocking any merge operations. Idempotent. */
    fun close() {
        callbackChannels.close()
    }

    /** Closes channels and cancels all session-scoped coroutines. Called on final session teardown. */
    fun terminate() {
        close()
        scope.cancel()
    }
}
