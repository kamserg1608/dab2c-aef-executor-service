package ru.sbrf.dab2c.executor.it.tests.grpc.proxy

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.coroutineScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.TestSession
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

class ProxyActiveConnectionsGaugeTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `grpc_connections_active gauge should reflect concurrent active connections`() = runItTest {
        coroutineScope {
            val session1 = TestSession(proxyStub())
            val session2 = TestSession(proxyStub())

            try {
                session1.start(this)
                session1.sendRequest(settingsRequest())
                mockGigaVoiceService.awaitRequest { it.hasSettings() }

                session2.start(this)
                session2.sendRequest(settingsRequest())
                mockGigaVoiceService.awaitRequest { it.hasSettings() }

                val metrics = fetchPrometheusMetrics()
                val activeValue = extractGaugeValue(metrics, METRIC_NAME)
                assertThat(activeValue).isEqualTo(2.0)
            } finally {
                session1.cancel()
                session2.cancel()
            }
        }

        val afterClose = fetchPrometheusMetrics()
        val afterCloseValue = extractGaugeValue(afterClose, METRIC_NAME)
        assertThat(afterCloseValue).isEqualTo(0.0)
    }

    private suspend fun fetchPrometheusMetrics(): String =
        httpClient.get("$basePath/actuator/prometheus") {
            header("Accept", "text/plain; version=0.0.4; charset=utf-8")
        }.bodyAsText()

    private fun extractGaugeValue(prometheusBody: String, metricName: String): Double {
        val regex = Regex("""${Regex.escape(metricName)}\{[^}]*}\s+(\S+)""")
        val match = regex.find(prometheusBody)
            ?: error("Metric $metricName not found in prometheus output")
        return match.groupValues[1].toDouble()
    }

    private companion object {
        private const val METRIC_NAME = "grpc_connections_active"
    }
}
