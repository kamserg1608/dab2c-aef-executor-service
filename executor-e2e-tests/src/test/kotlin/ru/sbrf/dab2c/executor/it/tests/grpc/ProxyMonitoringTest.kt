package ru.sbrf.dab2c.executor.it.tests.grpc

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.common.model.ClientMetric
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.inputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.metrics.PrometheusMetricsParser
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric

class ProxyMonitoringTest : BaseGigaVoiceIntegrationTest() {

    private val logger = KotlinLogging.logger { }

    // --- Helpers ---

    private suspend fun fetchMetricsBody(): String {
        return httpClient.get("/actuator/metrics") {
            header("Accept", "text/plain; version=0.0.4; charset=utf-8")
        }.bodyAsText()
    }

    private suspend fun parseMetrics(): PrometheusMetricsParser {
        val body = fetchMetricsBody()
        logger.debug { "Metrics response body:\n$body" }
        return PrometheusMetricsParser(body).also { it.debugPrint() }
    }

    private suspend fun getMetricValue(
        metricName: String,
        additionalTags: Map<String, String> = emptyMap()
    ): Double? {
        val allTags = BASE_TAGS + additionalTags
        return parseMetrics().findMetric(metricName, allTags)?.value
    }

    // --- Assertions ---

    private suspend fun assertMetricExists(metricName: String) {
        val metrics = parseMetrics()
        Assertions.assertThat(metrics.findAllMetricsByName(metricName)).isNotEmpty()
            .withFailMessage("Метрика '$metricName' не найдена в ответе.")
    }

    private suspend fun assertActiveConnections(expectedValue: Double) {
        val actual = getMetricValue(ExecutorVoiceMetric.GRPC_CONNECTIONS_ACTIVE.metricName)
        Assertions.assertThat(actual)
            .withFailMessage("Expected active connections = $expectedValue, got: $actual")
            .isEqualTo(expectedValue)
    }

    private suspend fun assertTimerSumPositive(baseMetricName: String) {
        val sumValue = getMetricValue("${baseMetricName}_sum")
        Assertions.assertThat(sumValue)
            .withFailMessage("Sum value for $baseMetricName must be > 0.0, got: $sumValue")
            .isNotNull()
            .isGreaterThan(0.0)
    }

    private suspend fun assertMetricHasTags(
        metricName: String,
        additionalTags: Map<String, String> = emptyMap()
    ) {
        val metrics = parseMetrics()
        val allExpectedTags = BASE_TAGS + additionalTags

        val metricLine = metrics.findMetric(metricName, allExpectedTags)

        val availableMetricsInfo = metrics.findAllMetricsByName(metricName)
            .joinToString("\n") { "  $it" }

        assertThat(metricLine)
            .withFailMessage(
                "Метрика '$metricName' не найдена с ожидаемыми тегами.\n" +
                    "Ожидаемые теги: $allExpectedTags\n" +
                    "Доступные метрики с этим именем:\n$availableMetricsInfo"
            )
            .isNotNull()

        logger.debug { "Метрика '$metricName' найдена с тегами: $allExpectedTags" }
    }

    private suspend fun verifyConnectionCountIncreased(
        metricName: String,
        expectedIncrement: Int = 1,
        previousValue: Double,
    ) {
        val currentValue = getMetricValue(metricName)

        Assertions.assertThat(currentValue)
            .withFailMessage("Метрика '$metricName' не обнаружена")
            .isNotNull()

        val expectedValue = previousValue + expectedIncrement
        Assertions.assertThat(currentValue!!)
            .withFailMessage(
                "Счетчик '$metricName' должен увеличиться на $expectedIncrement. " +
                    "Ожидалось: $expectedValue, фактически: $currentValue"
            )
            .isEqualTo(expectedValue)
    }

    /**
     * Функция получает начальное значение метрики и проверяет, что оно увеличилось.
     */
    private suspend fun assertTotalCounterIncreased(
        metricName: String,
        additionalTags: Map<String, String> = emptyMap(),
        assertionMessage: String = "Счетчик '$metricName' должен увеличиться",
        testBody: suspend () -> Unit
    ) {
        val initialValue = getMetricValue(metricName, additionalTags) ?: 0.0
        logger.debug { "Начальное значение $metricName: $initialValue" }

        testBody()

        assertMetricExists(metricName)
        assertMetricHasTags(metricName, additionalTags)

        val finalValue = getMetricValue(metricName, additionalTags)

        Assertions.assertThat(finalValue)
            .withFailMessage(
                "$assertionMessage. " +
                    "Начальное: $initialValue, финальное: $finalValue"
            )
            .isNotNull()
            .isGreaterThan(initialValue)
    }

    // --- Tests ---

    @Test
    fun `should contain metrics for incoming chunks in metrics endpoint`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest())
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(inputTranscriptionResponse("Hello"))
            session.awaitResponse { it.hasInputTranscription() }
        }

        assertMetricExists(ExecutorVoiceMetric.GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL.metricName)
    }

    @Test
    fun `should contain metrics for outgoing chunks in metrics endpoint`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest())
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(inputTranscriptionResponse("Hello"))
            session.awaitResponse { it.hasInputTranscription() }
        }

        assertMetricExists(ExecutorVoiceMetric.GRPC_OUTGOING_TO_INITIATOR_CHUNKS_TOTAL.metricName)
    }

    @Test
    fun `should contain metric for time to first non-technical chunk`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)
        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("backend-function-test"))
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()
            session.sendRequest(audioRequest(speechStart = true))
            mock.awaitRequest { it.hasInput() }
            mock.sendResponse(inputTranscriptionResponse())
        }

        assertTimerSumPositive(ExecutorVoiceMetric.GRPC_CONNECTIONS_TTFB_SECONDS.metricName)
    }

    @Test
    fun `should contain the duration of the grpc connection processing`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("backend-function-test"))
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()
        }

        assertTimerSumPositive(ExecutorVoiceMetric.GRPC_CONNECTIONS_DURATION_SECONDS.metricName)
    }

    @Test
    fun `should track active grpc connections count during session`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)
        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest())
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse())

            assertActiveConnections(1.0)
            session.awaitResponse()
        }
        assertActiveConnections(0.0)
    }

    @Test
    fun `should increase the total number of grpc connections metric`() = runItTest {
        val initialCount = getMetricValue(ExecutorVoiceMetric.GRPC_CONNECTIONS_TOTAL.metricName) ?: 0.0
        logger.debug { "Начальное значение grpc_connections_total: $initialCount" }

        setupStubs(efsAdapterMock, gigaVoiceAgentMock)
        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("backend-function-test"))
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()

            verifyConnectionCountIncreased(
                metricName = ExecutorVoiceMetric.GRPC_CONNECTIONS_TOTAL.metricName,
                expectedIncrement = 1,
                previousValue = initialCount,
            )
        }
    }

    @Test
    fun `should record the total number of tokens in the response from gigavoice`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val additionalTags = mapOf(
            "model" to "test-model",
            "stream_chunk_type" to "Output",
            "version" to "1.2.0"
        )

        assertTotalCounterIncreased(
            metricName = ExecutorVoiceMetric.GRPC_RESPONSE_TOKENS_TOTAL.metricName,
            additionalTags = additionalTags,
            assertionMessage = "Счетчик grpc_response_tokens_total должен увеличиться"
        ) {
            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest("backend-function-test"))
                mock.awaitRequest { it.hasSettings() }
                mock.sendResponse(outputTranscriptionResponse())
                session.awaitResponse()
                session.sendRequest(audioRequest(speechStart = true))
                mock.awaitRequest { it.hasInput() }
                mock.sendResponse(GigaVoiceResponseFixtures.additionalDataResponse())
                session.awaitResponse()
            }
        }
    }

    @Test
    fun `should contain a metric for the total number of chunks received from gigavoice`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val additionalTags = mapOf(
            "stream_chunk_type" to "GigaVoiceResponse",
        )

        assertTotalCounterIncreased(
            metricName = ExecutorVoiceMetric.GRPC_INCOMING_FROM_GIGAVOICE_CHUNKS_TOTAL.metricName,
            additionalTags = additionalTags,
            assertionMessage = "Счетчик grpc_incoming_from_gigavoice_chunks_total должен увеличиться"
        ) {
            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest("backend-function-test"))
                mock.awaitRequest { it.hasSettings() }
                mock.sendResponse(outputTranscriptionResponse())
                session.awaitResponse()
                session.sendRequest(audioRequest(speechStart = true))
                mock.awaitRequest { it.hasInput() }
                mock.sendResponse(GigaVoiceResponseFixtures.additionalDataResponse())
                session.awaitResponse()
            }
        }
    }

    @Test
    fun `should contain a metric for the total number of chunks sent to gigavoice`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val additionalTags = mapOf(
            "stream_chunk_type" to "GigaVoiceRequest",
        )

        assertTotalCounterIncreased(
            metricName = ExecutorVoiceMetric.GRPC_OUTGOING_FROM_GIGAVOICE_CHUNKS_TOTAL.metricName,
            additionalTags = additionalTags,
            assertionMessage = "Счетчик grpc_outcoming_from_gigavoice_chunks_total должен увеличиться"
        ) {
            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest("backend-function-test"))
                mock.awaitRequest { it.hasSettings() }
                mock.sendResponse(outputTranscriptionResponse())
                session.awaitResponse()
                session.sendRequest(audioRequest(speechStart = true))
                mock.awaitRequest { it.hasInput() }
                mock.sendResponse(GigaVoiceResponseFixtures.additionalDataResponse())
                session.awaitResponse()
            }
        }
    }

    @Test
    fun `should contain a metric for the total number of chunks sent back to the initiator`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val additionalTags = mapOf(
            "stream_chunk_type" to "OutputTranscription",
        )

        assertTotalCounterIncreased(
            metricName = ExecutorVoiceMetric.GRPC_OUTGOING_TO_INITIATOR_CHUNKS_TOTAL.metricName,
            additionalTags = additionalTags,
            assertionMessage = "Счетчик grpc_outgoing_to_initiator_chunks_total должен увеличиться"
        ) {
            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest("backend-function-test"))
                mock.awaitRequest { it.hasSettings() }
                mock.sendResponse(outputTranscriptionResponse())
                session.awaitResponse()
                session.sendRequest(audioRequest(speechStart = true))
                mock.awaitRequest { it.hasInput() }
                mock.sendResponse(GigaVoiceResponseFixtures.additionalDataResponse())
                session.awaitResponse()
            }
        }
    }

    @Test
    fun `should contain a metric for the total number of chunks received from the initiator`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val additionalTags = mapOf(
            "stream_chunk_type" to "Audio",
        )

        assertTotalCounterIncreased(
            metricName = ExecutorVoiceMetric.GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL.metricName,
            additionalTags = additionalTags,
            assertionMessage = "Счетчик grpc_incoming_from_initiator_chunks_total должен увеличиться"
        ) {
            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest("backend-function-test"))
                mock.awaitRequest { it.hasSettings() }
                mock.sendResponse(outputTranscriptionResponse())
                session.awaitResponse()
                session.sendRequest(audioRequest(speechStart = true))
                mock.awaitRequest { it.hasInput() }
                mock.sendResponse(GigaVoiceResponseFixtures.additionalDataResponse())
                session.awaitResponse()
            }
        }
    }

    @Test
    fun `should contain a metric for the duration of http request processing`() = runItTest {

        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("backend-function-test"))
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()
        }

        assertTimerSumPositive(ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS.metricName)
    }

    @Test
    fun `should contain a metric for the total http requests`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val additionalTags = mapOf(
            "destination_service" to "gigavoice-agent",
            "status_code" to "200",
            "endpoint" to "/settings",
            "method" to "post"
        )

        assertTotalCounterIncreased(
            metricName = ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL.metricName,
            additionalTags = additionalTags,
            assertionMessage = "Счетчик http_integration_requests_total должен увеличиться"
        ) {
            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest("backend-function-test"))
                mock.awaitRequest { it.hasSettings() }
                mock.sendResponse(outputTranscriptionResponse())
                session.awaitResponse()
                session.sendRequest(audioRequest(speechStart = true))
                mock.awaitRequest { it.hasInput() }
                mock.sendResponse(GigaVoiceResponseFixtures.additionalDataResponse())
                session.awaitResponse()
            }
        }
    }

    companion object {
        const val TEST_CHANNEL = "test-channel"
        const val TEST_PLATFORM = "test-platform"
        val BASE_TAGS = mapOf("channel" to TEST_CHANNEL, "platform" to TEST_PLATFORM)
    }
}
