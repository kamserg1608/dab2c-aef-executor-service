package ru.sbrf.dab2c.executor.it.tests.grpc.monitoring

import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.additionalDataResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.audioResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.metrics.captureMetrics
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric

/**
 * Response token counting metrics with model and version tags.
 */
class GrpcResponseTokensMetricsTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should count response tokens with model and version tags`() = runItTest {
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

                mock.sendResponse(additionalDataResponse())
                session.awaitResponse()
            }
        }

        capture.assertCounterIncreasedBy(
            ExecutorVoiceMetric.GRPC_RESPONSE_TOTAL_TOKENS.metricName,
            EXPECTED_TOTAL_TOKENS,
            mapOf(
                "stream_chunk_type" to "Output",
                "model" to "test-model",
                "version" to "1.2.0"
            )
        )
    }

    @Test
    fun `should not count tokens for Audio output chunk`() = runItTest {
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

                mock.sendResponse(audioResponse(1))
                session.awaitResponse()
            }
        }

        capture.assertCounterNotIncreased(
            ExecutorVoiceMetric.GRPC_RESPONSE_TOTAL_TOKENS.metricName,
            mapOf("stream_chunk_type" to "Output")
        )
    }

    companion object {
        private val BASE_TAGS = mapOf("channel" to "test-channel", "platform" to "test-platform")
        private const val EXPECTED_TOTAL_TOKENS = 300.0
    }
}
