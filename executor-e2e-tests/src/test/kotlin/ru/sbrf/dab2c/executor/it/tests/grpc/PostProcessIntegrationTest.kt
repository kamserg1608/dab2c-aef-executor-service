package ru.sbrf.dab2c.executor.it.tests.grpc

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import kotlinx.coroutines.delay
import org.apache.kafka.clients.consumer.Consumer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.functionCallingResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.kafka.KafkaTestSupport.consumeRecords
import ru.sbrf.dab2c.executor.it.support.kafka.KafkaTestSupport.createTestConsumer
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.TestSessionScope
import ru.sbrf.dab2c.executor.it.support.session.closeAndAwaitCompletion
import ru.sbrf.dab2c.executor.it.support.session.withAbortedSession
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.AuditEventAwaiter.awaitAuditEvent
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockAwaiter
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubsWithPostProcessing
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentFunctionsForName
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentPostProcessError
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentPostProcessWithAnalytics
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentPostProcessWithDelay
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentSettingsError
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentSettingsWithContext
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Verifies that a finished conversation reaches `/postprocess` with the accumulated context on both
 * the normal and the aborted completion path, that the call is skipped when it must be, and that
 * neither a slow nor a failing handle affects the session.
 */
class PostProcessIntegrationTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should call postprocess with the accumulated context when IVR closes the stream`() = runItTest {
        setupStubsWithPostProcessing(efsAdapterMock, gigaVoiceAgentMock)
        gigaVoiceAgentMock.stubGigaAgentSettingsWithContext(SETTINGS_CONTEXT)
        gigaVoiceAgentMock.stubGigaAgentFunctionsForName(FUNCTION_NAME, RESULT_CONTENT, FUNCTION_CONTEXT)
        val awaiter = WireMockAwaiter(gigaVoiceAgentMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            runDialogWithFunctionCall("postprocess-normal-close")
        }

        val body = ObjectMappers.MAPPER.readTree(awaiter.awaitPostCall(POSTPROCESS_URL).bodyAsString)
        assertThat(body.get("conversation_id").asText()).isEqualTo("postprocess-normal-close")
        assertThat(body.get("edu_id").asText()).isEqualTo("test-edu-id")
        assertThat(body.get("config")).isNotNull
        assertThat(body.get("context").toString()).isEqualTo(FUNCTION_CONTEXT)
    }

    @Test
    fun `should call postprocess when the live stream is aborted`() = runItTest {
        setupStubsWithPostProcessing(efsAdapterMock, gigaVoiceAgentMock)
        val awaiter = WireMockAwaiter(gigaVoiceAgentMock)

        withAbortedSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest(IVR_CONTEXT))
            session.sendRequest(settingsRequest("postprocess-aborted"))
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()
        }

        val body = ObjectMappers.MAPPER.readTree(awaiter.awaitPostCall(POSTPROCESS_URL).bodyAsString)
        assertThat(body.get("conversation_id").asText()).isEqualTo("postprocess-aborted")
        assertThat(body.get("context").toString()).isEqualTo(IVR_CONTEXT)
    }

    @Test
    fun `should close the response stream without waiting for a slow handle`() = runItTest {
        setupStubsWithPostProcessing(efsAdapterMock, gigaVoiceAgentMock)
        gigaVoiceAgentMock.stubGigaAgentPostProcessWithDelay(HELD_HANDLE_MS)
        val awaiter = WireMockAwaiter(gigaVoiceAgentMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            runDialog("postprocess-non-blocking")
            closeAndAwaitCompletion()
        }

        awaiter.awaitPostCall(POSTPROCESS_URL)
    }

    @Test
    fun `should neither call postprocess nor publish analytics when the toggle is off`() = runItTest {
        setupStubsWithPostProcessing(efsAdapterMock, gigaVoiceAgentMock, postProcessingEnabled = false)
        gigaVoiceAgentMock.stubGigaAgentPostProcessWithAnalytics(TOGGLE_OFF_ANALYTICS)
        val consumer = embeddedKafkaBroker.createTestConsumer(AGENTS_TOPIC)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            runDialog(TOGGLE_OFF_CONVERSATION)
        }

        assertNoPostProcessCall()
        assertNothingPublished(consumer, TOGGLE_OFF_CONVERSATION)
    }

    @Test
    fun `should not call postprocess when the session never reached serving`() = runItTest {
        setupStubsWithPostProcessing(efsAdapterMock, gigaVoiceAgentMock)
        gigaVoiceAgentMock.stubGigaAgentSettingsError()

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest(IVR_CONTEXT))
            session.sendRequest(settingsRequest("postprocess-no-serving"))

            assertThat(session.awaitResponse { it.hasError() }.error.status).isEqualTo(SETTINGS_ERROR_STATUS)
        }

        assertNoPostProcessCall()
    }

    @Test
    fun `should audit the failure and publish nothing when the handle answers with an error`() = runItTest {
        setupStubsWithPostProcessing(efsAdapterMock, gigaVoiceAgentMock)
        gigaVoiceAgentMock.stubGigaAgentPostProcessError()
        val awaiter = WireMockAwaiter(gigaVoiceAgentMock)
        val consumer = embeddedKafkaBroker.createTestConsumer(AGENTS_TOPIC)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            runDialog(HANDLE_ERROR_CONVERSATION)
        }

        awaiter.awaitPostCall(POSTPROCESS_URL)
        gigaVoiceAgentMock.verify(exactly(1), postRequestedFor(urlEqualTo(POSTPROCESS_URL)))
        assertPostProcessFailureAudited(HANDLE_ERROR_CONVERSATION)
        assertNothingPublished(consumer, HANDLE_ERROR_CONVERSATION)
    }

    @Test
    fun `should audit the failure and publish nothing when the handle answers slower than the timeout`() = runItTest {
        setupStubsWithPostProcessing(efsAdapterMock, gigaVoiceAgentMock)
        gigaVoiceAgentMock.stubGigaAgentPostProcessWithDelay(TIMED_OUT_HANDLE_MS)
        val consumer = embeddedKafkaBroker.createTestConsumer(AGENTS_TOPIC)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            runDialog(TIMEOUT_CONVERSATION)
        }

        assertPostProcessFailureAudited(TIMEOUT_CONVERSATION)
        gigaVoiceAgentMock.verify(exactly(1), postRequestedFor(urlEqualTo(POSTPROCESS_URL)))
        assertNothingPublished(consumer, TIMEOUT_CONVERSATION)
    }

    private suspend fun TestSessionScope.runDialog(voiceCallId: String): GigaVoiceResponse {
        session.sendRequest(contextRequest(IVR_CONTEXT))
        session.sendRequest(settingsRequest(voiceCallId))
        mock.awaitRequest { it.hasSettings() }

        mock.sendResponse(outputTranscriptionResponse())
        return session.awaitResponse()
    }

    private suspend fun TestSessionScope.runDialogWithFunctionCall(voiceCallId: String) {
        runDialog(voiceCallId)

        session.sendRequest(audioRequest(speechStart = true))
        mock.awaitRequest { it.hasInput() }

        mock.sendResponse(functionCallingResponse(FUNCTION_NAME, """{"account_id": "12345"}"""))
        mock.awaitRequest { it.hasFunctionResult() }
    }

    private suspend fun assertPostProcessFailureAudited(conversationId: String) {
        val params = efsAdapterMock.awaitAuditEvent(AGENT_INTERACTION_FAILED, timeout = AUDIT_AWAIT) {
            it["ERROR_CODE"] == POSTPROCESS_ERROR_CODE &&
                it["RQ_MESSAGE"]?.contains(""""conversationId":"$conversationId"""") == true
        }
        val rqMessage: Map<String, Any?> = ObjectMappers.MAPPER.readValue(params["RQ_MESSAGE"]!!)
        assertThat(rqMessage["endpoint"]).isEqualTo(POSTPROCESS_URL)
    }

    private suspend fun assertNothingPublished(consumer: Consumer<String, String>, conversationId: String) {
        val records = consumeRecords<AgentAnalyticsEnvelope>(
            consumer, AGENTS_TOPIC, timeout = SILENCE_POLL,
            filter = { it.data?.contains(""""conversation_id":"$conversationId"""") == true }
        )
        assertThat(records).isEmpty()
    }

    private suspend fun assertNoPostProcessCall() {
        delay(NO_CALL_GRACE_MS)
        gigaVoiceAgentMock.verify(exactly(0), postRequestedFor(urlEqualTo(POSTPROCESS_URL)))
    }

    private companion object {
        const val POSTPROCESS_URL = "/postprocess"
        const val FUNCTION_NAME = "get_account_balance"
        const val RESULT_CONTENT = """{"balance": 1000}"""
        const val IVR_CONTEXT = """{"ivr":true}"""
        const val SETTINGS_CONTEXT = """{"a":1}"""
        const val FUNCTION_CONTEXT = """{"b":2}"""
        const val HELD_HANDLE_MS = 30_000
        const val NO_CALL_GRACE_MS = 1000L
        const val SETTINGS_ERROR_STATUS = 1501
        const val AGENTS_TOPIC = "dab2c-agents"
        const val TOGGLE_OFF_ANALYTICS = """{"metric":"toggle-off"}"""
        const val TOGGLE_OFF_CONVERSATION = "postprocess-toggle-off"
        const val HANDLE_ERROR_CONVERSATION = "postprocess-handle-error"
        const val TIMEOUT_CONVERSATION = "postprocess-timeout"
        const val AGENT_INTERACTION_FAILED = "DAB2C_AGENT_INTERACTION_FAILED"
        const val POSTPROCESS_ERROR_CODE = "GIGAVOICE_POSTPROCESS_ERROR"
        const val TIMED_OUT_HANDLE_MS = 6000
        val AUDIT_AWAIT = 40.seconds
        val SILENCE_POLL: Duration = Duration.ofSeconds(2)
    }
}
