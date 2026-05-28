package ru.sbrf.dab2c.executor.it.tests.grpc.tracing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.functionCallingResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.inputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.platformFunctionProcessing
import ru.sbrf.dab2c.executor.it.support.kafka.TracingKafkaConsumer.collectTraceSpans
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentFunctions
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.testing.tracing.assertSpans

/** Function call dance produces a tool span plus an HTTP `/functions` output_request span. */
class TracingFunctionCallDanceTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `function call dance emits tool span and HTTP_functions output_request`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)
        gigaVoiceAgentMock.stubGigaAgentFunctions("get_account_balance", """{"balance": 1000}""")

        val spans = embeddedKafkaBroker.collectTraceSpans(voiceCallId = "function-dance") {
            runItTest {
                withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                    session.sendRequest(contextRequest())
                    session.sendRequest(settingsRequest("function-dance"))
                    mock.awaitRequest { it.hasSettings() }

                    mock.sendResponse(inputTranscriptionResponse("balance please"))
                    session.awaitResponse { it.hasInputTranscription() }
                    mock.sendResponse(outputTranscriptionResponse("checking"))
                    session.awaitResponse { it.hasOutputTranscription() }

                    session.sendRequest(audioRequest(speechStart = true))
                    mock.awaitRequest { it.hasInput() }

                    mock.sendResponse(
                        functionCallingResponse("get_account_balance", """{"account_id": "12345"}""")
                    )
                    mock.sendResponse(platformFunctionProcessing())
                    session.awaitResponse { it.hasPlatformFunctionProcessing() }
                    wireMock.awaitPostCall("/functions")

                    mock.sendResponse(outputTranscriptionResponse("your balance is 1000"))
                    session.awaitResponse { it.hasOutputTranscription() }
                }
            }
        }

        assertThat(spans).isNotEmpty
        assertSpans(spans) {
            val tool = allOfKind("tool").firstOrNull { it.name == "get_account_balance" }
            assertThat(tool).withFailMessage("Expected tool span for get_account_balance").isNotNull
            assertAttributeNonEmpty(tool!!, "aef.input")
            assertAttributeNonEmpty(tool, "aef.output")

            val functionsHttp = allOfKind("output_request").firstOrNull { it.name == "agent /functions" }
            assertThat(functionsHttp).withFailMessage("Expected HTTP output_request for /functions").isNotNull
            assertAttributeNonEmpty(functionsHttp!!, "aef.request.body")

            assertNestedUnder(tool, "voice_llm_turn")
            assertNestedUnder(functionsHttp, "tool")
            assertAttributeEquals(tool, "aef.session_id", "test-session-id")
            assertAttributeEquals(functionsHttp, "aef.session_id", "test-session-id")
        }
    }
}
