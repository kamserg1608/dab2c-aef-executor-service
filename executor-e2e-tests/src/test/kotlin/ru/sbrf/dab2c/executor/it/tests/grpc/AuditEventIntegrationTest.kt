package ru.sbrf.dab2c.executor.it.tests.grpc

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.errorResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.functionCallingResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.inputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.warningResponse
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockAwaiter
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentFunctions
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.library.testing.golden.assertMatchesGolden
import ru.sbrf.dab2c.executor.library.testing.golden.maskNonDeterministic

/**
 * Integration tests for audit event emission.
 * Verifies per-turn external interaction audits, agent interaction audits,
 * tolerant exception handling, error handling, cookie propagation, and resilience to audit failures.
 */
class AuditEventIntegrationTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should emit per-turn external interaction audits on multi-turn conversation`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)
        val auditAwaiter = WireMockAwaiter(efsAdapterMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("audit-multi-turn"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(inputTranscriptionResponse("Hello"))
            session.awaitResponse { it.hasInputTranscription() }

            mock.sendResponse(outputTranscriptionResponse("World"))
            session.awaitResponse { it.hasOutputTranscription() }

            mock.sendResponse(inputTranscriptionResponse("Next"))
            session.awaitResponse { it.hasInputTranscription() }

            mock.sendResponse(outputTranscriptionResponse("Response"))
            session.awaitResponse { it.hasOutputTranscription() }

            mock.sendResponse(inputTranscriptionResponse("Third"))
            session.awaitResponse { it.hasInputTranscription() }
        }

        val auditRequests = auditAwaiter.awaitPostCalls(AUDIT_EVENT_URL, 4)
        val externalAudits = findAllAuditEvents(auditRequests, "DAB2C_EXTERNAL_INTERACTION")

        assertThat(externalAudits).hasSize(3)

        val firstTurn = externalAudits[0]
        assertThat(firstTurn.success()).isTrue()
        assertThat(firstTurn.params()["ANSWER_CODE"]).isEqualTo("200")
        assertThat(firstTurn.params()["SENDER"]).isEqualTo("dab2c-aef-executor")
        assertThat(firstTurn.params()["RECEIVER"]).isEqualTo("voice-external")
        assertThat(firstTurn.params()["RQ_MESSAGE"]).isEqualTo("Hello")
        assertThat(firstTurn.params()["RS_MESSAGE"]).isEqualTo("World")

        val secondTurn = externalAudits[1]
        assertThat(secondTurn.params()["RQ_MESSAGE"]).isEqualTo("Next")
        assertThat(secondTurn.params()["RS_MESSAGE"]).isEqualTo("Response")

        val incompleteTurn = externalAudits[2]
        assertThat(incompleteTurn.params()["RQ_MESSAGE"]).isEqualTo("Third")
        assertThat(incompleteTurn.params()).doesNotContainKey("RS_MESSAGE")
    }

    @Test
    fun `should emit per-turn audit for completed turn with warning`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)
        val auditAwaiter = WireMockAwaiter(efsAdapterMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("audit-warning"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(inputTranscriptionResponse("Hello"))
            session.awaitResponse { it.hasInputTranscription() }

            mock.sendResponse(warningResponse("High latency"))
            session.awaitResponse { it.hasWarning() }

            mock.sendResponse(outputTranscriptionResponse("World"))
            session.awaitResponse { it.hasOutputTranscription() }
        }

        val auditRequests = auditAwaiter.awaitPostCalls(AUDIT_EVENT_URL, 2)
        val externalAudit = findAuditEvent(auditRequests, "DAB2C_EXTERNAL_INTERACTION")

        assertThat(externalAudit).isNotNull
        assertThat(externalAudit!!.params()["RQ_MESSAGE"]).isEqualTo("Hello")
        assertThat(externalAudit.params()["RS_MESSAGE"]).isEqualTo("World")
    }

    @Test
    fun `should emit per-turn audit for completed turn with error response`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)
        val auditAwaiter = WireMockAwaiter(efsAdapterMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("audit-error"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(inputTranscriptionResponse("Hello"))
            session.awaitResponse { it.hasInputTranscription() }

            mock.sendResponse(errorResponse(503, "unavailable"))
            session.awaitResponse { it.hasError() }

            mock.sendResponse(outputTranscriptionResponse("Sorry"))
            session.awaitResponse { it.hasOutputTranscription() }
        }

        val auditRequests = auditAwaiter.awaitPostCalls(AUDIT_EVENT_URL, 2)
        val externalAudit = findAuditEvent(auditRequests, "DAB2C_EXTERNAL_INTERACTION")

        assertThat(externalAudit).isNotNull
        assertThat(externalAudit!!.params()["RQ_MESSAGE"]).isEqualTo("Hello")
        assertThat(externalAudit.params()["RS_MESSAGE"]).isEqualTo("Sorry")
    }

    @Test
    fun `should emit per-turn success audit and session failed audit when downstream stream errors`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)
        val auditAwaiter = WireMockAwaiter(efsAdapterMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("audit-stream-error"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(inputTranscriptionResponse("Hello"))
            session.awaitResponse { it.hasInputTranscription() }

            mock.sendResponse(outputTranscriptionResponse("World"))
            session.awaitResponse { it.hasOutputTranscription() }

            mock.completeResponsesWithError(RuntimeException("stream failure"))
        }

        val auditRequests = auditAwaiter.awaitPostCalls(AUDIT_EVENT_URL, 3)

        val successAudit = findAuditEvent(auditRequests, "DAB2C_EXTERNAL_INTERACTION")
        assertThat(successAudit).isNotNull
        assertThat(successAudit!!.success()).isTrue()
        assertThat(successAudit.params()["RQ_MESSAGE"]).isEqualTo("Hello")
        assertThat(successAudit.params()["RS_MESSAGE"]).isEqualTo("World")

        val failedAudit = findAuditEvent(auditRequests, "DAB2C_EXTERNAL_INTERACTION_FAILED")
        assertThat(failedAudit).isNotNull
        assertThat(failedAudit!!.success()).isFalse()
        val params = failedAudit.params()
        assertThat(params["ANSWER_CODE"]).isEqualTo("500")
        assertThat(params["ERROR_CODE"]).isNotBlank()
        assertThat(params["ERROR_TITLE"]).isNotBlank()
    }

    @Test
    fun `should not emit failed audit when stream closes with CANCELLED status`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)
        val auditAwaiter = WireMockAwaiter(efsAdapterMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("audit-cancelled"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(inputTranscriptionResponse("Hello"))
            session.awaitResponse { it.hasInputTranscription() }

            mock.sendResponse(outputTranscriptionResponse("World"))
            session.awaitResponse { it.hasOutputTranscription() }

            mock.completeResponsesWithError(StatusRuntimeException(Status.CANCELLED.withDescription("RPC cancelled")))
        }

        val auditRequests = auditAwaiter.awaitPostCalls(AUDIT_EVENT_URL, 2)

        val successAudit = findAuditEvent(auditRequests, "DAB2C_EXTERNAL_INTERACTION")
        assertThat(successAudit).isNotNull
        assertThat(successAudit!!.success()).isTrue()

        delay(500)
        val allAuditRequests = efsAdapterMock.findAll(postRequestedFor(urlEqualTo(AUDIT_EVENT_URL)))
        val failedAudit = findAuditEvent(allAuditRequests, "DAB2C_EXTERNAL_INTERACTION_FAILED")
        assertThat(failedAudit).isNull()
    }

    // --- Agent Interaction Audit Tests ---

    @Test
    fun `should emit agent interaction audit with request and response content on settings call`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)
        val auditAwaiter = WireMockAwaiter(efsAdapterMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("audit-agent-settings"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()
        }

        val auditRequests = auditAwaiter.awaitPostCalls(AUDIT_EVENT_URL, 2)
        val agentAudit = findAuditEvent(auditRequests, "DAB2C_AGENT_INTERACTION")

        assertThat(agentAudit).isNotNull
        assertThat(agentAudit!!.success()).isTrue()
        val params = agentAudit.params()
        assertThat(params["ANSWER_CODE"]).isEqualTo("200")
        assertThat(params["SENDER"]).isEqualTo("dab2c-aef-executor")
        assertThat(params["RECEIVER"]).isEqualTo("giga-voice-agent")

        val rqMessage: Map<String, Any?> = ObjectMappers.MAPPER.readValue(params["RQ_MESSAGE"]!!)
        val rsMessage: Map<String, Any?> = ObjectMappers.MAPPER.readValue(params["RS_MESSAGE"]!!)

        assertMatchesGolden(
            maskNonDeterministic(rqMessage, AUDIT_NON_DETERMINISTIC_FIELDS),
            "golden/audit/settings-rq.json"
        )
        assertMatchesGolden(
            maskNonDeterministic(rsMessage, AUDIT_NON_DETERMINISTIC_FIELDS),
            "golden/audit/settings-rs.json"
        )
    }

    @Test
    fun `should emit failed agent interaction audit when GigaAgent settings returns 500`() = runItTest {
        efsAdapterMock.stubFor(
            post(urlEqualTo("/configurator/rest-agent"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(EFS_ADAPTER_RESPONSE)
                )
        )

        gigaVoiceAgentMock.stubFor(
            post(urlEqualTo("/settings"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")
                )
        )

        val auditAwaiter = WireMockAwaiter(efsAdapterMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("audit-agent-error"))

            session.awaitResponse { it.hasError() }
        }

        val auditRequests = auditAwaiter.awaitPostCalls(AUDIT_EVENT_URL, 1)
        val failedAudit = findAuditEvent(auditRequests, "DAB2C_AGENT_INTERACTION_FAILED")

        assertThat(failedAudit).isNotNull
        assertThat(failedAudit!!.success()).isFalse()
        val params = failedAudit.params()
        assertThat(params["ANSWER_CODE"]).isEqualTo("500")
        assertThat(params["ERROR_CODE"]).isEqualTo("GIGAVOICE_SETTINGS_ERROR")
        assertThat(params["ERROR_TITLE"]).isNotBlank()
        assertThat(params["SENDER"]).isEqualTo("dab2c-aef-executor")
        assertThat(params["RECEIVER"]).isEqualTo("giga-voice-agent")
        val rqMessage: Map<String, Any?> = ObjectMappers.MAPPER.readValue(params["RQ_MESSAGE"]!!)
        assertThat(rqMessage["endpoint"]).isEqualTo("/settings")
    }

    @Test
    fun `should emit agent interaction audit for backend function execution`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)
        gigaVoiceAgentMock.stubGigaAgentFunctions("get_account_balance", """{"balance": 1000}""")
        val auditAwaiter = WireMockAwaiter(efsAdapterMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("audit-function-call"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()

            session.sendRequest(audioRequest(speechStart = true))
            mock.awaitRequest { it.hasInput() }

            mock.sendResponse(functionCallingResponse("get_account_balance", """{"account_id": "12345"}"""))

            auditAwaiter.awaitPostCalls(AUDIT_EVENT_URL, 2)
        }

        val auditRequests = auditAwaiter.awaitPostCalls(AUDIT_EVENT_URL, 2)
        val functionAudit = auditRequests
            .map { parseAuditEvent(it) }
            .find { event ->
                event.eventName() == "DAB2C_AGENT_INTERACTION" &&
                    event.params()["RQ_MESSAGE"]?.contains("/functions") == true
            }

        assertThat(functionAudit).isNotNull
        assertThat(functionAudit!!.success()).isTrue()
        assertThat(functionAudit.params()["ANSWER_CODE"]).isEqualTo("200")
        assertThat(functionAudit.params()["SENDER"]).isEqualTo("dab2c-aef-executor")
        assertThat(functionAudit.params()["RECEIVER"]).isEqualTo("giga-voice-agent")

        val rqMessage: Map<String, Any?> = ObjectMappers.MAPPER.readValue(functionAudit.params()["RQ_MESSAGE"]!!)
        val rsMessage: Map<String, Any?> = ObjectMappers.MAPPER.readValue(functionAudit.params()["RS_MESSAGE"]!!)

        assertMatchesGolden(
            maskNonDeterministic(rqMessage, AUDIT_NON_DETERMINISTIC_FIELDS),
            "golden/audit/function-call-rq.json"
        )
        assertMatchesGolden(
            maskNonDeterministic(rsMessage, AUDIT_NON_DETERMINISTIC_FIELDS),
            "golden/audit/function-call-rs.json"
        )
    }

    @Test
    fun `should emit failed agent interaction audit when GigaAgent functions returns 500`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)

        gigaVoiceAgentMock.stubFor(
            post(urlEqualTo("/functions"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")
                )
        )

        val auditAwaiter = WireMockAwaiter(efsAdapterMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("audit-function-error"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()

            session.sendRequest(audioRequest(speechStart = true))
            mock.awaitRequest { it.hasInput() }

            mock.sendResponse(functionCallingResponse("get_account_balance", """{"account_id": "12345"}"""))

            auditAwaiter.awaitPostCalls(AUDIT_EVENT_URL, 2)
        }

        val auditRequests = auditAwaiter.awaitPostCalls(AUDIT_EVENT_URL, 2)
        val failedFunctionAudit = auditRequests
            .map { parseAuditEvent(it) }
            .find { event ->
                event.eventName() == "DAB2C_AGENT_INTERACTION_FAILED" &&
                    event.params()["RQ_MESSAGE"]?.contains("/functions") == true
            }

        assertThat(failedFunctionAudit).isNotNull
        assertThat(failedFunctionAudit!!.success()).isFalse()
        assertThat(failedFunctionAudit.params()["ERROR_CODE"]).isEqualTo("GIGAVOICE_FUNCTION_ERROR")
        assertThat(failedFunctionAudit.params()["SENDER"]).isEqualTo("dab2c-aef-executor")
        assertThat(failedFunctionAudit.params()["RECEIVER"]).isEqualTo("giga-voice-agent")
        assertThat(failedFunctionAudit.params()["ANSWER_CODE"]).isEqualTo("500")
        assertThat(failedFunctionAudit.params()["ERROR_TITLE"]).isNotBlank()
        val rqMessage: Map<String, Any?> = ObjectMappers.MAPPER.readValue(failedFunctionAudit.params()["RQ_MESSAGE"]!!)
        assertThat(rqMessage["endpoint"]).isEqualTo("/functions")
    }

    // --- Cross-Cutting Tests ---

    @Test
    fun `should include UFS session and token cookies on audit requests`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)
        val auditAwaiter = WireMockAwaiter(efsAdapterMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("audit-cookies"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()
        }

        val auditRequests = auditAwaiter.awaitPostCalls(AUDIT_EVENT_URL, 2)
        auditRequests.forEach { request ->
            val cookie = request.getHeader("Cookie")
            assertThat(cookie).contains("UFS-SESSION=test-session")
            assertThat(cookie).contains("UFS-TOKEN=test-token")
        }
    }

    @Test
    fun `should continue main flow when audit endpoint returns 500`() = runItTest {
        efsAdapterMock.stubFor(
            post(urlEqualTo("/audit/event"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")
                )
        )

        setupStubsWithoutAudit()

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("audit-resilience"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse("Hello from downstream"))
            val response = session.awaitResponse()

            assertThat(response).isNotNull
            assertThat(response.outputTranscription.text).isEqualTo("Hello from downstream")
        }
    }

    // --- Helpers ---

    private fun findAuditEvent(requests: List<LoggedRequest>, eventName: String): AuditEventBody? =
        requests
            .map { parseAuditEvent(it) }
            .find { it.eventName() == eventName }

    private fun findAllAuditEvents(requests: List<LoggedRequest>, eventName: String): List<AuditEventBody> =
        requests
            .map { parseAuditEvent(it) }
            .filter { it.eventName() == eventName }

    private fun parseAuditEvent(request: LoggedRequest): AuditEventBody =
        AuditEventBody(ObjectMappers.MAPPER.readValue(request.bodyAsString))

    private fun setupStubsWithoutAudit() {
        efsAdapterMock.stubFor(
            post(urlEqualTo("/configurator/rest-agent"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(EFS_ADAPTER_RESPONSE)
                )
        )
        gigaVoiceAgentMock.stubFor(
            post(urlEqualTo("/settings"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(GIGA_VOICE_SETTINGS_RESPONSE)
                )
        )
    }

    private class AuditEventBody(private val json: Map<String, Any?>) {
        fun eventName(): String? = json["event"] as? String
        fun success(): Boolean? = json["success"] as? Boolean

        @Suppress("UNCHECKED_CAST")
        fun params(): Map<String, String> =
            json["params"] as? Map<String, String> ?: emptyMap()
    }

    companion object {
        private const val AUDIT_EVENT_URL = "/audit/event"

        /** Non-deterministic fields in audit payloads: wiremock port + per-call timestamps. */
        private val AUDIT_NON_DETERMINISTIC_FIELDS = setOf("receiver", "timestamp")

        private val EFS_ADAPTER_RESPONSE = """
            {
                "success": true,
                "body": {
                    "test-agent": {
                        "name": "test-agent",
                        "type": "voice",
                        "functional_subsystem_ci": "test-ci",
                        "description": "Test agent",
                        "entry_points": [],
                        "ufs_service_available": true,
                        "can_access_user_info": true,
                        "tools_meta": [],
                        "neighbours_agent_meta": [],
                        "toggles": {}
                    }
                }
            }
        """.trimIndent()

        private val GIGA_VOICE_SETTINGS_RESPONSE = """
            {
                "settings": {
                    "voice_call_id": "test-call-123",
                    "audio": {}
                },
                "performers": {
                    "functions": []
                }
            }
        """.trimIndent()
    }
}
