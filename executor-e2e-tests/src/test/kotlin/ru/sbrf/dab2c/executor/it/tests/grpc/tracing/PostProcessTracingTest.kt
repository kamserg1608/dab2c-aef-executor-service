package ru.sbrf.dab2c.executor.it.tests.grpc.tracing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.kafka.TracingKafkaConsumer.collectAllSpans
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubsWithPostProcessing
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentSettingsWithCallId
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.testing.tracing.ParsedSpan

/**
 * The detached `/postprocess` call is traced inside the session it belongs to: its span hangs off
 * the same `start_agent` span as the session, which is already closed by the time the call runs.
 * Assertions walk that subtree — the tracing topic carries every concurrent call in a single trace.
 */
class PostProcessTracingTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should export the postprocess span under the start_agent span of its own session`() = runItTest {
        setupStubsWithPostProcessing(efsAdapterMock, gigaVoiceAgentMock)
        gigaVoiceAgentMock.stubGigaAgentSettingsWithCallId(CALL_ID)

        val spans = embeddedKafkaBroker.collectAllSpans(awaitSpan = ::isOurPostProcessSpan) {
            runItTest {
                withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                    session.sendRequest(contextRequest())
                    session.sendRequest(settingsRequest(CALL_ID))
                    mock.awaitRequest { it.hasSettings() }
                    mock.sendResponse(outputTranscriptionResponse("hi user"))
                    session.awaitResponse { it.hasOutputTranscription() }
                }
            }
        }

        val postProcess = spans.single(::isOurPostProcessSpan)
        assertThat(postProcess.attributes["aef.request.method"]).isEqualTo("POST")
        assertThat(postProcess.attributes["aef.request.path"]).isEqualTo("/postprocess")
        assertThat(postProcess.attributes["aef.response.body"]).isNotBlank()

        val voiceSession = spans.voiceSessionOf(CALL_ID)
        val startAgent = spans.parentOf(spans.parentOf(voiceSession))
        assertThat(startAgent.kind()).isEqualTo("start_agent")
        assertThat(postProcess.parentSpanId).isEqualTo(startAgent.spanId)
    }

    private fun isOurPostProcessSpan(span: ParsedSpan): Boolean =
        span.name == SPAN_NAME &&
            span.attributes["aef.request.body"]?.contains(""""conversationId":"$CALL_ID"""") == true

    private fun List<ParsedSpan>.voiceSessionOf(voiceCallId: String): ParsedSpan =
        single {
            it.kind() == "voice_session" &&
                it.attributes["aef.settings"]?.contains(""""voiceCallId":"$voiceCallId"""") == true
        }

    private fun List<ParsedSpan>.parentOf(span: ParsedSpan): ParsedSpan =
        single { it.spanId == span.parentSpanId }

    private companion object {
        const val SPAN_NAME = "agent /postprocess"
        const val CALL_ID = "vc-postprocess-tracing"
    }
}
