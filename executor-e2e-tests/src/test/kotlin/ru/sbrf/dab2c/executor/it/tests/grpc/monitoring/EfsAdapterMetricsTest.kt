package ru.sbrf.dab2c.executor.it.tests.grpc.monitoring

import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.common.model.ClientMetric
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.metrics.captureMetrics
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

/**
 * HTTP integration metrics for EFS Adapter calls (configurator, session, profile, SDS, audit).
 */
class EfsAdapterMetricsTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should record counter and timer for EFS configurator getRestAgentConfig call`() = runItTest {
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
                    "destination_service" to "efs-adapter",
                    "endpoint" to "/configurator/rest-agent",
                    "method" to "post",
                    "status_code" to "200"
                )
            )
            .assertTimerRecorded(
                ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS.metricName,
                mapOf(
                    "destination_service" to "efs-adapter",
                    "endpoint" to "/configurator/rest-agent",
                    "method" to "post",
                    "status_code" to "200"
                )
            )
    }

    companion object {
        private val BASE_TAGS = mapOf("channel" to "test-channel", "platform" to "test-platform")
    }
}
