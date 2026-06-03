package ru.sbrf.dab2c.executor.it.tests.grpc.tracing

import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
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

        val traceparent = gigaVoiceAgentMock
            .findAll(postRequestedFor(urlEqualTo("/functions")))
            .first()
            .getHeader("traceparent")
        assertThat(traceparent).withFailMessage("Agent /functions call must carry W3C traceparent").isNotNull
        assertThat(traceparent).matches("00-[0-9a-f]{32}-[0-9a-f]{16}-[0-9a-f]{2}")
        val parts = traceparent.split("-")
        assertThat(parts[1]).`as`("non-zero trace-id").isNotEqualTo("0".repeat(32))
        assertThat(parts[2]).`as`("non-zero parent span-id").isNotEqualTo("0".repeat(16))
    }

    @Test
    fun `function_call without preceding output_transcription still nests tool under voice_llm_turn`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)
        gigaVoiceAgentMock.stubGigaAgentFunctions("get_account_balance", """{"balance": 1000}""")

        val spans = embeddedKafkaBroker.collectTraceSpans(voiceCallId = "function-nogap") {
            runItTest {
                withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                    session.sendRequest(contextRequest())
                    session.sendRequest(settingsRequest("function-nogap"))
                    mock.awaitRequest { it.hasSettings() }

                    mock.sendResponse(inputTranscriptionResponse("check my profile"))
                    session.awaitResponse { it.hasInputTranscription() }

                    mock.sendResponse(functionCallingResponse("get_account_balance", """{"account_id": "12345"}"""))
                    mock.sendResponse(platformFunctionProcessing())
                    session.awaitResponse { it.hasPlatformFunctionProcessing() }
                    wireMock.awaitPostCall("/functions")

                    mock.sendResponse(outputTranscriptionResponse("could not check your profile"))
                    session.awaitResponse { it.hasOutputTranscription() }
                }
            }
        }

        assertThat(spans).isNotEmpty
        assertSpans(spans) {
            val tool = allOfKind("tool").firstOrNull { it.name == "get_account_balance" }
            assertThat(tool).withFailMessage("Expected tool span for get_account_balance").isNotNull
            assertNestedUnder(tool!!, "voice_llm_turn")

            val functionsHttp = allOfKind("output_request").firstOrNull { it.name == "agent /functions" }
            assertThat(functionsHttp).withFailMessage("Expected HTTP output_request for /functions").isNotNull
            assertNestedUnder(functionsHttp!!, "tool")
        }
    }
}
