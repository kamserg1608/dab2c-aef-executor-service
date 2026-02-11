package ru.sbrf.dab2c.executor.it.tests.grpc.proxy

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.audioResponse
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric

class ProxyMonitoringTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should be metrics for incoming and outcoming chunks`() = runItTest {

        GigaVoiceResponse.newBuilder()
        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(settingsRequest())
            mock.awaitRequest { it.hasSettings() }
            val audioResponse = audioResponse(1)
            mock.sendResponse(audioResponse)
            session.awaitResponse()
        }

        val response = httpClient.get("$basePath/actuator/metrics")

        print(response.bodyAsText())

        assertThat(
            response.bodyAsText().contains(ExecutorVoiceMetric.GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL.metricName)
        ).isTrue()
        assertThat(
            response.bodyAsText().contains(ExecutorVoiceMetric.GRPC_OUTGOING_FROM_INITIATOR_CHUNKS_TOTAL.metricName)
        ).isTrue()
    }

    @Test
    fun `should contain metrics for incoming and outgoing chunks in prometheus endpoint`() = runItTest {
        GigaVoiceResponse.newBuilder()
        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(settingsRequest())
            mock.awaitRequest { it.hasSettings() }
            val audioResponse = audioResponse(1)
            mock.sendResponse(audioResponse)
            session.awaitResponse()
        }

        val response = httpClient.get("$basePath/actuator/prometheus") {
            header("Accept", "text/plain; version=0.0.4; charset=utf-8")
        }

        val responseBody = response.bodyAsText()

        print(responseBody)

        assertThat(responseBody).contains(ExecutorVoiceMetric.GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL.metricName)
        assertThat(responseBody).contains(ExecutorVoiceMetric.GRPC_OUTGOING_FROM_INITIATOR_CHUNKS_TOTAL.metricName)
    }
}
