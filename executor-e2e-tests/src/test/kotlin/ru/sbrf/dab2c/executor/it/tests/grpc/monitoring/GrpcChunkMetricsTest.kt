package ru.sbrf.dab2c.executor.it.tests.grpc.monitoring

import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.functionResultRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.additionalDataResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.errorResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.functionCallingResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.inputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.warningResponse
import ru.sbrf.dab2c.executor.it.support.metrics.captureMetrics
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentFunctions
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentSettingsWithProfanityCheck
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric

/**
 * Chunk counting metrics for all four gRPC chunk counters:
 * incoming_from_initiator, outgoing_to_initiator, outgoing_to_gigavoice, incoming_from_gigavoice.
 */
class GrpcChunkMetricsTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should count Context, Settings, and Audio chunks from initiator`() = runItTest {
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

        capture
            .assertCounterIncreased(
                ExecutorVoiceMetric.GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL.metricName,
                mapOf("stream_chunk_type" to "Context", "function_name" to "")
            )
            .assertCounterIncreased(
                ExecutorVoiceMetric.GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL.metricName,
                mapOf("stream_chunk_type" to "Settings", "function_name" to "")
            )
            .assertCounterIncreased(
                ExecutorVoiceMetric.GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL.metricName,
                mapOf("stream_chunk_type" to "Audio", "function_name" to "")
            )
    }

    @Test
    fun `should count FunctionResult chunk from initiator with function_name`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)

        val capture = captureMetrics(httpClient, BASE_TAGS) {
            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest("ivr-function-test"))
                mock.awaitRequest { it.hasSettings() }

                mock.sendResponse(outputTranscriptionResponse())
                session.awaitResponse()

                session.sendRequest(audioRequest(speechStart = true))
                mock.awaitRequest { it.hasInput() }

                mock.sendResponse(
                    functionCallingResponse("transfer_to_operator", """{"reason": "customer request"}""")
                )
                session.awaitResponse { it.hasFunctionCall() }

                session.sendRequest(functionResultRequest("transfer_to_operator", """{"status": "ok"}"""))
                mock.awaitRequest { it.hasFunctionResult() }
            }
        }

        capture.assertCounterIncreased(
            ExecutorVoiceMetric.GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL.metricName,
            mapOf("stream_chunk_type" to "FunctionResult", "function_name" to "transfer_to_operator")
        )
    }

    @Test
    fun `should count OutputTranscription, InputTranscription, and Output chunks from gigavoice`() = runItTest {
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

                mock.sendResponse(additionalDataResponse())
                session.awaitResponse()
            }
        }

        capture
            .assertCounterIncreased(
                ExecutorVoiceMetric.GRPC_INCOMING_FROM_GIGAVOICE_CHUNKS_TOTAL.metricName,
                mapOf("stream_chunk_type" to "OutputTranscription", "function_name" to "")
            )
            .assertCounterIncreased(
                ExecutorVoiceMetric.GRPC_INCOMING_FROM_GIGAVOICE_CHUNKS_TOTAL.metricName,
                mapOf("stream_chunk_type" to "InputTranscription", "function_name" to "")
            )
            .assertCounterIncreased(
                ExecutorVoiceMetric.GRPC_INCOMING_FROM_GIGAVOICE_CHUNKS_TOTAL.metricName,
                mapOf("stream_chunk_type" to "Output", "function_name" to "")
            )
    }

    @Test
    fun `should count FunctionCalling chunk from gigavoice with function_name`() = runItTest {
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

        capture.assertCounterIncreased(
            ExecutorVoiceMetric.GRPC_INCOMING_FROM_GIGAVOICE_CHUNKS_TOTAL.metricName,
            mapOf("stream_chunk_type" to "FunctionCalling", "function_name" to "get_account_balance")
        )
    }

    @Test
    fun `should count Warning and Error chunks from gigavoice`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val capture = captureMetrics(httpClient, BASE_TAGS) {
            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest())
                mock.awaitRequest { it.hasSettings() }

                mock.sendResponse(warningResponse())
                session.awaitResponse()

                mock.sendResponse(errorResponse())
                session.awaitResponse()
            }
        }

        capture
            .assertCounterIncreased(
                ExecutorVoiceMetric.GRPC_INCOMING_FROM_GIGAVOICE_CHUNKS_TOTAL.metricName,
                mapOf("stream_chunk_type" to "Warning", "function_name" to "")
            )
            .assertCounterIncreased(
                ExecutorVoiceMetric.GRPC_INCOMING_FROM_GIGAVOICE_CHUNKS_TOTAL.metricName,
                mapOf("stream_chunk_type" to "Error", "function_name" to "")
            )
    }

    @Test
    fun `should count OutputTranscription and InputTranscription chunks sent to initiator`() = runItTest {
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

        capture
            .assertCounterIncreased(
                ExecutorVoiceMetric.GRPC_OUTGOING_TO_INITIATOR_CHUNKS_TOTAL.metricName,
                mapOf("stream_chunk_type" to "OutputTranscription")
            )
            .assertCounterIncreased(
                ExecutorVoiceMetric.GRPC_OUTGOING_TO_INITIATOR_CHUNKS_TOTAL.metricName,
                mapOf("stream_chunk_type" to "InputTranscription")
            )
    }

    @Test
    fun `should count FunctionCalling chunk sent to initiator for IVR function`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)

        val capture = captureMetrics(httpClient, BASE_TAGS) {
            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest("ivr-function-test"))
                mock.awaitRequest { it.hasSettings() }

                mock.sendResponse(outputTranscriptionResponse())
                session.awaitResponse()

                session.sendRequest(audioRequest(speechStart = true))
                mock.awaitRequest { it.hasInput() }

                mock.sendResponse(
                    functionCallingResponse("transfer_to_operator", """{"reason": "customer request"}""")
                )
                session.awaitResponse { it.hasFunctionCall() }
            }
        }

        capture.assertCounterIncreased(
            ExecutorVoiceMetric.GRPC_OUTGOING_TO_INITIATOR_CHUNKS_TOTAL.metricName,
            mapOf("stream_chunk_type" to "FunctionCalling")
        )
    }

    @Test
    fun `should count Warning and Error chunks sent to initiator`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val capture = captureMetrics(httpClient, BASE_TAGS) {
            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest())
                mock.awaitRequest { it.hasSettings() }

                mock.sendResponse(warningResponse())
                session.awaitResponse()

                mock.sendResponse(errorResponse())
                session.awaitResponse()
            }
        }

        capture
            .assertCounterIncreased(
                ExecutorVoiceMetric.GRPC_OUTGOING_TO_INITIATOR_CHUNKS_TOTAL.metricName,
                mapOf("stream_chunk_type" to "Warning")
            )
            .assertCounterIncreased(
                ExecutorVoiceMetric.GRPC_OUTGOING_TO_INITIATOR_CHUNKS_TOTAL.metricName,
                mapOf("stream_chunk_type" to "Error")
            )
    }

    @Test
    fun `should count chunks sent to gigavoice with profanity_check tag value`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)
        gigaVoiceAgentMock.stubGigaAgentSettingsWithProfanityCheck()

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

        capture.assertCounterIncreased(
            ExecutorVoiceMetric.GRPC_OUTGOING_TO_GIGAVOICE_CHUNKS_TOTAL.metricName,
            mapOf("stream_chunk_type" to "Settings", "profanity_check" to "true")
        )
    }

    @Test
    fun `should count Settings and Audio chunks sent to gigavoice`() = runItTest {
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

        capture
            .assertCounterIncreased(
                ExecutorVoiceMetric.GRPC_OUTGOING_TO_GIGAVOICE_CHUNKS_TOTAL.metricName,
                mapOf("stream_chunk_type" to "Settings", "profanity_check" to "")
            )
            .assertCounterIncreased(
                ExecutorVoiceMetric.GRPC_OUTGOING_TO_GIGAVOICE_CHUNKS_TOTAL.metricName,
                mapOf("stream_chunk_type" to "Audio", "profanity_check" to "")
            )
    }

    @Test
    fun `should record settings initialization duration timer`() = runItTest {
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
            .assertTimerRecorded(ExecutorVoiceMetric.GRPC_SETTINGS_INITIALIZATION_DURATION_SECONDS.metricName)
            .assertTimerRecorded(ExecutorVoiceMetric.GRPC_SESSION_INITIALIZATION_DURATION_SECONDS.metricName)
    }

    companion object {
        private val BASE_TAGS = mapOf("channel" to "test-channel", "platform" to "test-platform")
    }
}
