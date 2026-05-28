package ru.sbrf.dab2c.executor.it.tests.grpc.tracing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.inputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.kafka.TracingKafkaConsumer.collectTraceSpans
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.testing.tracing.assertSpans

/** Single user-turn golden path — full span hierarchy is published to the AEF tracing topic. */
class TracingGoldenPathTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `single turn produces full span hierarchy`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val spans = embeddedKafkaBroker.collectTraceSpans(voiceCallId = "vc-golden") {
            runItTest {
                withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                    session.sendRequest(contextRequest())
                    session.sendRequest(settingsRequest("vc-golden"))
                    mock.awaitRequest { it.hasSettings() }

                    mock.sendResponse(inputTranscriptionResponse("hello"))
                    session.awaitResponse { it.hasInputTranscription() }

                    mock.sendResponse(outputTranscriptionResponse("hi user"))
                    session.awaitResponse { it.hasOutputTranscription() }
                }
            }
        }

        assertThat(spans).isNotEmpty
        assertSpans(spans) {
            val inputRequest = ofKind("input_request")
            val startAgent = ofKind("start_agent")
            assertNestedUnder(startAgent, "input_request")
            val voiceSession = ofKind("voice_session")
            val downstream = allOfKind("output_request").first { it.name == "downstream gigavoice stream" }
            assertNestedUnder(voiceSession, "output_request")
            assertNestedUnder(downstream, "start_agent")
            val voiceTurn = ofKind("voice_turn")
            assertNestedUnder(voiceTurn, "voice_session")
            val llmTurn = ofKind("voice_llm_turn")
            assertNestedUnder(llmTurn, "voice_turn")
            assertAttributeNonEmpty(voiceSession, "aef.settings")
            assertAttributeNonEmpty(inputRequest, "aef.request.path")
            assertAttributeEquals(
                downstream, "aef.request.path", "GigaVoiceProtocol.GigaVoiceService/GigaVoice"
            )

            val settingsHttp = allOfKind("output_request").first { it.name == "agent /settings" }
            assertNestedUnder(settingsHttp, "start_agent")
            listOf(inputRequest, startAgent, voiceSession, downstream, voiceTurn, llmTurn, settingsHttp)
                .forEach { assertAttributeEquals(it, "aef.session_id", "test-session-id") }
        }
    }
}
