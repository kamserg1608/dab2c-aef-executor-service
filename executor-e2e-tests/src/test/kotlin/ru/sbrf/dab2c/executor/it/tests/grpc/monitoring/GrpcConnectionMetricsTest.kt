package ru.sbrf.dab2c.executor.it.tests.grpc.monitoring

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.inputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.metrics.MetricsCapture
import ru.sbrf.dab2c.executor.it.support.metrics.captureMetrics
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric

/**
 * Connection lifecycle metrics: total, active, duration, and time-to-first-byte.
 */
class GrpcConnectionMetricsTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should track connection lifecycle - total, active, and duration`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val beforeMetrics = MetricsCapture.fetchAndParse(httpClient)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest())
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(outputTranscriptionResponse())

            val duringMetrics = MetricsCapture.fetchAndParse(httpClient)
            val activeMetricName = ExecutorVoiceMetric.GRPC_CONNECTIONS_ACTIVE.metricName
            val activeValue = duringMetrics.findMetric(activeMetricName, BASE_TAGS)?.value
            assertThat(activeValue).isEqualTo(1.0)

            session.awaitResponse()
        }

        val afterMetrics = MetricsCapture.fetchAndParse(httpClient)
        val capture = MetricsCapture(beforeMetrics, afterMetrics, BASE_TAGS)

        capture
            .assertCounterIncreased(ExecutorVoiceMetric.GRPC_CONNECTIONS_TOTAL.metricName)
            .assertGaugeEquals(ExecutorVoiceMetric.GRPC_CONNECTIONS_ACTIVE.metricName, 0.0)
            .assertTimerRecorded(ExecutorVoiceMetric.GRPC_CONNECTIONS_DURATION_SECONDS.metricName)
    }

    @Test
    fun `should record time to first InputTranscription`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val capture = captureMetrics(httpClient, BASE_TAGS) {
            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest())
                mock.awaitRequest { it.hasSettings() }

                mock.sendResponse(outputTranscriptionResponse())
                session.awaitResponse()

                session.sendRequest(audioRequest(speechStart = true))
                mock.awaitRequest { it.hasInput() }

                mock.sendResponse(inputTranscriptionResponse())
                session.awaitResponse { it.hasInputTranscription() }
            }
        }

        capture.assertTimerRecorded(ExecutorVoiceMetric.GRPC_CONNECTIONS_TTFB_SECONDS.metricName)
    }

    companion object {
        private val BASE_TAGS = mapOf("channel" to "test-channel", "platform" to "test-platform")
    }
}
