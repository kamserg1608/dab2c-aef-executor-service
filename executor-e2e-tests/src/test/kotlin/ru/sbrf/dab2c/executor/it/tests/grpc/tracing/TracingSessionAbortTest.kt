package ru.sbrf.dab2c.executor.it.tests.grpc.tracing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.functionCallingResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.inputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.kafka.TracingKafkaConsumer.collectTraceSpans
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.TestSessionScope
import ru.sbrf.dab2c.executor.it.support.session.withAbortedSession
import ru.sbrf.dab2c.executor.it.support.tracing.downstreamOf
import ru.sbrf.dab2c.executor.it.support.tracing.subtreeOf
import ru.sbrf.dab2c.executor.it.support.tracing.voiceSessionOf
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentFunctionsWithDelay
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentSettingsWithCallId
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.testing.tracing.ParsedSpan
import kotlin.time.Duration.Companion.seconds

/**
 * A call aborted mid-dialog still closes and exports its session spans.
 * Assertions walk the subtree of this call's own `voice_session`: the tracing topic carries
 * every concurrent call in a single trace.
 */
class TracingSessionAbortTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `abort after a completed turn still exports root session spans`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)
        gigaVoiceAgentMock.stubGigaAgentSettingsWithCallId(ABORT_TERMINAL_CALL_ID)

        val spans = collectAbortedSessionSpans(ABORT_TERMINAL_CALL_ID) {
            mock.sendResponse(inputTranscriptionResponse("hello"))
            session.awaitResponse { it.hasInputTranscription() }
            mock.sendResponse(outputTranscriptionResponse("hi user"))
            session.awaitResponse { it.hasOutputTranscription() }
        }

        val voiceSession = spans.voiceSessionOf(ABORT_TERMINAL_CALL_ID)
        assertThat(voiceSession.name).isEqualTo("voice session")
        assertThat(spans.downstreamOf(voiceSession).name).isEqualTo(DOWNSTREAM_SPAN)
    }

    @Test
    fun `abort inside the function call dance closes the dangling tool span`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)
        gigaVoiceAgentMock.stubGigaAgentSettingsWithCallId(ABORT_FUNCTION_CALL_ID, withFunctions = true)
        gigaVoiceAgentMock.stubGigaAgentFunctionsWithDelay(FUNCTION_NAME, FUNCTION_RESULT, FUNCTION_DELAY_MS)

        val spans = collectAbortedSessionSpans(ABORT_FUNCTION_CALL_ID) {
            mock.sendResponse(inputTranscriptionResponse("balance please"))
            session.awaitResponse { it.hasInputTranscription() }
            mock.sendResponse(functionCallingResponse(FUNCTION_NAME, """{"account_id": "12345"}"""))
            wireMock.awaitPostCall("/functions")
        }

        val voiceSession = spans.voiceSessionOf(ABORT_FUNCTION_CALL_ID)
        assertThat(spans.downstreamOf(voiceSession).name).isEqualTo(DOWNSTREAM_SPAN)
        val subtree = spans.subtreeOf(voiceSession)
        assertThat(subtree.filter { it.kind() == "voice_turn" }).hasSize(1)
        val llmTurns = subtree.filter { it.kind() == "voice_llm_turn" }
        assertThat(llmTurns).hasSize(1)
        val tools = subtree.filter { it.kind() == "tool" }
        assertThat(tools).hasSize(1)
        assertThat(tools.single().name).isEqualTo(FUNCTION_NAME)
        assertThat(tools.single().parentSpanId).isEqualTo(llmTurns.single().spanId)
    }

    private suspend fun collectAbortedSessionSpans(
        voiceCallId: String,
        dialog: suspend TestSessionScope.() -> Unit
    ): List<ParsedSpan> {
        val spans = embeddedKafkaBroker.collectTraceSpans(voiceCallId = voiceCallId) {
            withAbortedSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest(voiceCallId))
                mock.awaitRequest(timeout = SETTINGS_TIMEOUT) { it.hasSettings() }
                dialog()
            }
        }
        return spans
    }

    private companion object {
        const val ABORT_TERMINAL_CALL_ID = "abort-terminal"
        const val ABORT_FUNCTION_CALL_ID = "abort-function-dance"
        const val DOWNSTREAM_SPAN = "downstream gigavoice stream"
        const val FUNCTION_NAME = "get_account_balance"
        const val FUNCTION_RESULT = """{"balance": 1000}"""
        const val FUNCTION_DELAY_MS = 2000
        val SETTINGS_TIMEOUT = 15.seconds
    }
}
