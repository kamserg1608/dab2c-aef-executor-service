package ru.sbrf.dab2c.executor.it.tests.grpc.proxy

import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.serialization.jackson.jackson
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
    fun `should contain metrics for incoming and outgoing chunks in metrics endpoint`() = runItTest {
        GigaVoiceResponse.newBuilder()
        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(settingsRequest())
            mock.awaitRequest { it.hasSettings() }
            val audioResponse = audioResponse(1)
            mock.sendResponse(audioResponse)
            session.awaitResponse()
        }

        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson {
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
            }
            defaultRequest {
                url.protocol = URLProtocol.HTTP
                url.host = "localhost"
                url.port = 8081
            }
        }

        val response = client.get("/actuator/metrics") {
            header("Accept", "text/plain; version=0.0.4; charset=utf-8")
        }

        val responseBody = response.bodyAsText()

        print(responseBody)

        assertThat(responseBody).contains(ExecutorVoiceMetric.GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL.metricName)
        assertThat(responseBody).contains(ExecutorVoiceMetric.GRPC_OUTGOING_FROM_INITIATOR_CHUNKS_TOTAL.metricName)
    }
}
