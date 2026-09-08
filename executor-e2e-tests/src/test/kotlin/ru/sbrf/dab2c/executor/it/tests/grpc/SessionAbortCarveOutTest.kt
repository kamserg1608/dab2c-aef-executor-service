package ru.sbrf.dab2c.executor.it.tests.grpc

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.common.model.ClientMetric
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.functionCallingResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.metrics.MetricsCapture
import ru.sbrf.dab2c.executor.it.support.metrics.captureMetricsAwaitingCounter
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withAbortedSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentFunctionsWithDelay
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentSettingsWithDelay
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * An agent call still in flight when the IVR stream is aborted is treated as a cancellation:
 * no failed audit event is emitted and the integration metric carries `status_code="cancelled"`.
 */
class SessionAbortCarveOutTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should not audit and should record cancelled metric when settings is aborted`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)
        gigaVoiceAgentMock.stubGigaAgentSettingsWithDelay(AGENT_DELAY_MS)

        val capture = captureMetricsAwaitingCounter(
            httpClient,
            BASE_TAGS,
            ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL.metricName,
            cancelledTags(SETTINGS_SERVICE, SETTINGS_ENDPOINT)
        ) {
            withAbortedSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest("abort-during-settings"))
                wireMock.awaitPostCall(SETTINGS_ENDPOINT)
            }
        }

        assertNoFailedAgentAudit(SETTINGS_ERROR_CODE)
        assertCancelledNotException(capture, SETTINGS_SERVICE, SETTINGS_ENDPOINT)
    }

    @Test
    fun `should not audit and should record cancelled metric when function call is aborted`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)
        gigaVoiceAgentMock.stubGigaAgentFunctionsWithDelay(FUNCTION_NAME, FUNCTION_RESULT, AGENT_DELAY_MS)

        val capture = captureMetricsAwaitingCounter(
            httpClient,
            BASE_TAGS,
            ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL.metricName,
            cancelledTags(FUNCTIONS_SERVICE, FUNCTIONS_ENDPOINT)
        ) {
            withAbortedSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest("abort-during-function"))
                mock.awaitRequest { it.hasSettings() }

                mock.sendResponse(outputTranscriptionResponse())
                session.awaitResponse()

                session.sendRequest(audioRequest(speechStart = true))
                mock.awaitRequest { it.hasInput() }

                mock.sendResponse(functionCallingResponse(FUNCTION_NAME, """{"account_id": "12345"}"""))
                wireMock.awaitPostCall(FUNCTIONS_ENDPOINT)
            }
        }

        assertNoFailedAgentAudit(FUNCTION_ERROR_CODE)
        assertCancelledNotException(capture, FUNCTIONS_SERVICE, FUNCTIONS_ENDPOINT)
    }

    @Suppress("UNCHECKED_CAST")
    private fun assertNoFailedAgentAudit(errorCode: String) {
        val failedAudits = efsAdapterMock.findAll(postRequestedFor(urlEqualTo(AUDIT_EVENT_URL)))
            .map { ObjectMappers.MAPPER.readValue<Map<String, Any?>>(it.bodyAsString) }
            .filter { it["event"] == AGENT_INTERACTION_FAILED }
            .map { it["params"] as? Map<String, String> ?: emptyMap() }
            .filter { it["ERROR_CODE"] == errorCode }

        assertThat(failedAudits)
            .withFailMessage("Aborted agent call must not emit $AGENT_INTERACTION_FAILED with $errorCode")
            .isEmpty()
    }

    private fun assertCancelledNotException(
        capture: MetricsCapture,
        destinationService: String,
        endpoint: String
    ) {
        capture
            .assertCounterIncreased(
                ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL.metricName,
                callTags(destinationService, endpoint, "cancelled")
            )
            .assertCounterNotIncreased(
                ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL.metricName,
                callTags(destinationService, endpoint, "exception")
            )
    }

    private fun cancelledTags(destinationService: String, endpoint: String): Map<String, String> =
        callTags(destinationService, endpoint, "cancelled")

    private fun callTags(
        destinationService: String,
        endpoint: String,
        statusCode: String
    ): Map<String, String> =
        mapOf(
            "destination_service" to destinationService,
            "endpoint" to endpoint,
            "method" to "post",
            "status_code" to statusCode
        )

    private companion object {
        const val AUDIT_EVENT_URL = "/audit/event"
        const val AGENT_INTERACTION_FAILED = "DAB2C_AGENT_INTERACTION_FAILED"
        const val SETTINGS_ENDPOINT = "/settings"
        const val FUNCTIONS_ENDPOINT = "/functions"
        const val SETTINGS_SERVICE = "gigavoice-agent"
        const val FUNCTIONS_SERVICE = "ab-ivr"
        const val SETTINGS_ERROR_CODE = "GIGAVOICE_SETTINGS_ERROR"
        const val FUNCTION_ERROR_CODE = "GIGAVOICE_FUNCTION_ERROR"
        const val FUNCTION_NAME = "get_account_balance"
        const val FUNCTION_RESULT = """{"balance": 1000}"""
        const val AGENT_DELAY_MS = 5000

        val BASE_TAGS = mapOf("channel" to "test-channel", "platform" to "test-platform")
    }
}
