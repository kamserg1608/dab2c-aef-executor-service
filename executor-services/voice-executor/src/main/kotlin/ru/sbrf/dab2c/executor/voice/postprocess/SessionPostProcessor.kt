package ru.sbrf.dab2c.executor.voice.postprocess

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.slf4j.MDCContext
import ru.sbrf.dab2c.executor.library.context.HeadersElement
import ru.sbrf.dab2c.executor.library.context.SessionInfoElement
import ru.sbrf.dab2c.executor.library.tracing.TracingParentElement
import ru.sbrf.dab2c.executor.library.tracing.aef.AefRequestContextElement
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.VoiceSession
import ru.sbrf.dab2c.executor.voice.model.VoiceSessionFeatureTogglesElement
import ru.sbrf.dab2c.executor.voice.model.currentFeatureToggles
import ru.sbrf.dab2c.executor.voice.session.observer.VoiceSessionObserver
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

/** Never suspends, so it delays neither the observer chain nor the teardown of the gRPC stream. */
class SessionPostProcessor(
    private val session: VoiceSession,
    private val runner: PostProcessRunner
) : VoiceSessionObserver {

    override suspend fun onSessionCompleted(cause: Throwable?) {
        val state = session.state.value as? ProcessingState.Serving
        if (state == null) {
            logger.info { "Post-processing skipped: session never reached Serving" }
            return
        }
        if (!currentFeatureToggles().postProcessingEnabled) {
            logger.info { "Post-processing skipped for conversation ${state.conversationId}: toggle is off" }
            return
        }

        runner.submit(
            PostProcessSnapshot(
                conversationId = state.conversationId,
                agentConfiguration = state.agentConfiguration,
                contextData = state.contextData,
                assistantMessageId = session.turnIds.assistantMessageId
            ),
            detachedContext()
        )
    }

    /**
     * Carries over only what the detached task may safely outlive the session with. The gRPC call
     * object and the closed server span are deliberately left behind.
     */
    private suspend fun detachedContext(): CoroutineContext {
        val current = currentCoroutineContext()
        return CARRIED_KEYS.fold(Dispatchers.IO + MDCContext()) { context, key ->
            current[key]?.let { context + it } ?: context
        }
    }

    private companion object {
        val CARRIED_KEYS: List<CoroutineContext.Key<*>> = listOf(
            HeadersElement,
            SessionInfoElement,
            VoiceSessionFeatureTogglesElement,
            AefRequestContextElement,
            TracingParentElement
        )
    }
}
