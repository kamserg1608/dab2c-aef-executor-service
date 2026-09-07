package ru.sbrf.dab2c.executor.voice.postprocess

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.Parameter
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.library.context.HeadersElement
import ru.sbrf.dab2c.executor.library.context.SessionInfoElement
import ru.sbrf.dab2c.executor.library.tracing.TracingParentElement
import ru.sbrf.dab2c.executor.library.tracing.aef.AefRequestContextElement
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.VoiceSession
import ru.sbrf.dab2c.executor.voice.model.VoiceSessionFeatureToggles
import ru.sbrf.dab2c.executor.voice.model.VoiceSessionFeatureTogglesElement
import ru.sbrf.dab2c.executor.voice.service.impl.DialogContextTestFixtures.CONVERSATION_ID
import ru.sbrf.dab2c.executor.voice.service.impl.DialogContextTestFixtures.agentConfiguration
import ru.sbrf.dab2c.executor.voice.service.impl.DialogContextTestFixtures.dialogContext
import ru.sbrf.dab2c.executor.voice.service.impl.DialogContextTestFixtures.sessionContext
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * Verifies when [SessionPostProcessor] hands a session to the runner and what it carries over
 * into the detached context.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionPostProcessorTest {

    private val runner: PostProcessRunner = mockk(relaxed = true)
    private val session = VoiceSession()
    private val processor = SessionPostProcessor(session, runner)

    private val contextData = dialogContext("""{"accumulated":true}""")

    @Test
    fun `should submit a snapshot of the serving state when the toggle is on`() = runTest {
        session.state.value = servingState()
        val snapshot = slot<PostProcessSnapshot>()

        withContext(sessionContext(postProcessingEnabled = true)) {
            processor.onSessionCompleted(null)
        }

        verify { runner.submit(capture(snapshot), any()) }
        assertThat(snapshot.captured).isEqualTo(
            PostProcessSnapshot(
                CONVERSATION_ID, agentConfiguration, contextData, session.turnIds.assistantMessageId
            )
        )
    }

    @Test
    fun `should not submit when the session never reached serving`() = runTest {
        session.state.value = ProcessingState.AwaitingSettings(contextData)

        withContext(sessionContext(postProcessingEnabled = true)) {
            processor.onSessionCompleted(null)
        }

        verify(exactly = 0) { runner.submit(any(), any()) }
    }

    @Test
    fun `should not submit when the toggle is off`() = runTest {
        session.state.value = servingState()

        withContext(sessionContext(postProcessingEnabled = false)) {
            processor.onSessionCompleted(null)
        }

        verify(exactly = 0) { runner.submit(any(), any()) }
    }

    @Test
    fun `should submit when the session ends with a failure`() = runTest {
        session.state.value = servingState()

        withContext(sessionContext(postProcessingEnabled = true)) {
            processor.onSessionCompleted(IllegalStateException("downstream is gone"))
        }

        verify { runner.submit(any(), any()) }
    }

    @Test
    fun `should not submit when the toggle is absent in EFS`() = runTest {
        session.state.value = servingState()
        val absent = VoiceSessionFeatureToggles(
            mapOf(
                VoiceSessionFeatureToggles.POSTPROCESSING_ENABLED to
                    Parameter(VoiceSessionFeatureToggles.POSTPROCESSING_ENABLED, null)
            )
        )

        withContext(sessionContext(postProcessingEnabled = true) + VoiceSessionFeatureTogglesElement(absent)) {
            processor.onSessionCompleted(null)
        }

        verify(exactly = 0) { runner.submit(any(), any()) }
    }

    @Test
    fun `should carry session elements over but leave the caller job and unrelated elements behind`() = runTest {
        session.state.value = servingState()
        val context = slot<CoroutineContext>()

        val sessionContext = sessionContext(postProcessingEnabled = true) +
            AefRequestContextElement() +
            TracingParentElement() +
            CoroutineName("session-collector")

        withContext(sessionContext) {
            processor.onSessionCompleted(null)
        }

        verify { runner.submit(any(), capture(context)) }
        assertThat(context.captured[HeadersElement]).isNotNull
        assertThat(context.captured[SessionInfoElement]).isNotNull
        assertThat(context.captured[VoiceSessionFeatureTogglesElement]).isNotNull
        assertThat(context.captured[AefRequestContextElement]).isNotNull
        assertThat(context.captured[TracingParentElement]).isNotNull
        assertThat(context.captured[CoroutineName]).isNull()
        assertThat(context.captured[Job]).isNull()
        assertThat(context.captured[ContinuationInterceptor]).isEqualTo(Dispatchers.IO)
    }

    @Test
    fun `should submit from an already cancelled session context`() = runTest {
        session.state.value = servingState()

        val collector = launch(sessionContext(postProcessingEnabled = true)) {
            try {
                awaitCancellation()
            } finally {
                processor.onSessionCompleted(CancellationException("IVR is gone"))
            }
        }
        runCurrent()
        collector.cancelAndJoin()

        verify { runner.submit(any(), any()) }
    }

    private fun servingState() = ProcessingState.Serving(
        contextData = contextData,
        agentConfiguration = agentConfiguration,
        conversationId = CONVERSATION_ID,
        functionRegistry = FunctionPerformers(emptyMap())
    )
}
