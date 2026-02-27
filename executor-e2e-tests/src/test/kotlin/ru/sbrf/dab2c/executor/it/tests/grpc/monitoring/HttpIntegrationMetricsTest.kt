package ru.sbrf.dab2c.executor.it.tests.grpc.monitoring

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.common.model.ClientMetric
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.functionCallingResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.metrics.captureMetrics
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentFunctions
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

/**
 * HTTP integration metrics for /settings and /functions endpoints.
 */
class HttpIntegrationMetricsTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should record counter and timer for getSettings call`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val capture = captureMetrics(httpClient, BASE_TAGS) {
            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest())
                mock.awaitRequest { it.hasSettings() }
                mock.sendResponse(outputTranscriptionResponse())
                session.awaitResponse()
            }
        }

        capture
            .assertCounterIncreased(
                ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL.metricName,
                mapOf(
                    "destination_service" to "gigavoice-agent",
                    "endpoint" to "/settings",
                    "method" to "post",
                    "status_code" to "200"
                )
            )
            .assertTimerRecorded(
                ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS.metricName,
                mapOf(
                    "destination_service" to "gigavoice-agent",
                    "endpoint" to "/settings",
                    "method" to "post",
                    "status_code" to "200"
                )
            )
    }

    @Test
    fun `should record counter and timer for executeFunctionCall`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)
        gigaVoiceAgentMock.stubGigaAgentFunctions("get_account_balance", """{"balance": 1000}""")

        val capture = captureMetrics(httpClient, BASE_TAGS) {
            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest("backend-function-test"))
                mock.awaitRequest { it.hasSettings() }

                mock.sendResponse(outputTranscriptionResponse())
                session.awaitResponse()

                session.sendRequest(audioRequest(speechStart = true))
                mock.awaitRequest { it.hasInput() }

                mock.sendResponse(
                    functionCallingResponse("get_account_balance", """{"account_id": "12345"}""")
                )
                wireMock.awaitPostCall("/functions")
                mock.awaitRequest { it.hasFunctionResult() }
            }
        }

        capture
            .assertCounterIncreased(
                ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL.metricName,
                mapOf(
                    "destination_service" to "ab-ivr",
                    "endpoint" to "/functions",
                    "method" to "post",
                    "status_code" to "200"
                )
            )
            .assertTimerRecorded(
                ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS.metricName,
                mapOf(
                    "destination_service" to "ab-ivr",
                    "endpoint" to "/functions",
                    "method" to "post",
                    "status_code" to "200"
                )
            )
    }

    @Test
    fun `should record counter with error status_code when settings call fails`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)
        gigaVoiceAgentMock.stubFor(
            post(urlEqualTo("/settings"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error": "internal error"}""")
                )
        )

        val capture = captureMetrics(httpClient, BASE_TAGS) {
            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest())
                session.awaitResponse()
            }
        }

        capture
            .assertCounterIncreased(
                ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL.metricName,
                mapOf(
                    "destination_service" to "gigavoice-agent",
                    "endpoint" to "/settings",
                    "method" to "post",
                    "status_code" to "exception"
                )
            )
            .assertTimerRecorded(
                ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS.metricName,
                mapOf(
                    "destination_service" to "gigavoice-agent",
                    "endpoint" to "/settings",
                    "method" to "post",
                    "status_code" to "exception"
                )
            )
    }

    companion object {
        private val BASE_TAGS = mapOf("channel" to "test-channel", "platform" to "test-platform")
    }
}
