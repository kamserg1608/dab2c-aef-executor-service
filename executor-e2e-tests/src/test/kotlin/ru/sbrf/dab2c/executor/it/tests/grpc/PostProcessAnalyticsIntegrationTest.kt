package ru.sbrf.dab2c.executor.it.tests.grpc

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.common.model.ClientMetric
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsData
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.kafka.KafkaTestSupport.consumeRecords
import ru.sbrf.dab2c.executor.it.support.kafka.KafkaTestSupport.createTestConsumer
import ru.sbrf.dab2c.executor.it.support.metrics.MetricsCapture
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockAwaiter
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockResponses
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubsWithPostProcessing
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentPostProcessWithAnalytics
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Verifies that the analytics returned by `/postprocess` reach the KAP agents topic, one event per
 * list entry, and that the detached call is counted as an HTTP integration request.
 */
class PostProcessAnalyticsIntegrationTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should publish one analytics event per entry and count the call`() = runItTest {
        setupStubsWithPostProcessing(efsAdapterMock, gigaVoiceAgentMock)
        gigaVoiceAgentMock.stubGigaAgentPostProcessWithAnalytics(ANALYTICS_DATA)
        val awaiter = WireMockAwaiter(gigaVoiceAgentMock)

        val before = MetricsCapture.fetchAndParse(httpClient)
        val consumer = embeddedKafkaBroker.createTestConsumer(AGENTS_TOPIC)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest(CONVERSATION_ID))
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()
        }
        awaiter.awaitPostCall(POSTPROCESS_URL)

        val records = consumeRecords<AgentAnalyticsEnvelope>(
            consumer, AGENTS_TOPIC,
            filter = { it.data?.contains(ANALYTICS_MARKER) == true }
        )
        val after = MetricsCapture.fetchAndParse(httpClient)

        val published = records.map { ObjectMappers.MAPPER.readValue<AgentAnalyticsData>(it.data!!) }
        assertThat(published).hasSize(2)
        assertThat(published.map { it.dataVersion }).containsExactlyInAnyOrder(
            WireMockResponses.POSTPROCESS_DATA_VERSION_FIRST,
            WireMockResponses.POSTPROCESS_DATA_VERSION_SECOND
        )
        assertThat(published).allSatisfy {
            assertThat(it.conversationId).isEqualTo(CONVERSATION_ID)
            assertThat(it.agentName).isEqualTo("test-agent")
            assertThat(it.agentCi).isEqualTo("test-ci")
            assertThat(it.data).isEqualTo(ANALYTICS_DATA)
        }

        MetricsCapture(before, after, BASE_TAGS).assertCounterIncreased(
            ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL.metricName,
            mapOf(
                "destination_service" to "gigavoice-agent",
                "endpoint" to POSTPROCESS_URL,
                "method" to "post",
                "status_code" to "200"
            )
        )
    }

    private companion object {
        const val AGENTS_TOPIC = "dab2c-agents"
        const val POSTPROCESS_URL = "/postprocess"
        const val CONVERSATION_ID = "postprocess-analytics"
        const val ANALYTICS_MARKER = "postprocess-analytics-marker"
        const val ANALYTICS_DATA = """{"metric":"$ANALYTICS_MARKER","count":7}"""
        val BASE_TAGS = mapOf("channel" to "test-channel", "platform" to "test-platform")
    }
}
