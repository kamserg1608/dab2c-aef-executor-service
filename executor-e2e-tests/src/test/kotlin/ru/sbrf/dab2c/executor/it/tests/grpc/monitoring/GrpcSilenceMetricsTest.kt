package ru.sbrf.dab2c.executor.it.tests.grpc.monitoring

import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.audioResponse
import ru.sbrf.dab2c.executor.it.support.metrics.captureMetrics
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric

/**
 * Silence time metric: time between client speech_end and the first Output response.
 */
class GrpcSilenceMetricsTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should record silence time between speech_end and Output response`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val capture = captureMetrics(httpClient, BASE_TAGS) {
            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest())
                mock.awaitRequest { it.hasSettings() }

                session.sendRequest(audioRequest(speechEnd = true))
                mock.awaitRequest { it.hasInput() }

                mock.sendResponse(audioResponse(chunkId = 1))
                session.awaitResponse()
            }
        }

        capture.assertTimerRecorded(
            ExecutorVoiceMetric.GRPC_ASSISTANT_RESPONSE_SILENCE_TIME_SECONDS.metricName
        )
    }

    companion object {
        private val BASE_TAGS = mapOf("channel" to "test-channel", "platform" to "test-platform")
    }
}
