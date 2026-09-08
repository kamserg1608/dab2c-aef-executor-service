package ru.sbrf.dab2c.executor.clients.http.factory

import com.sun.net.httpserver.HttpServer
import io.ktor.client.request.post
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.net.InetSocketAddress
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class HttpClientAgentIdTest {

    @Test
    fun `sends agent id from Spring configuration to EFS adapter`() {
        assertThat(receiveAgentIds("CI15146365")).containsExactly("CI15146365")
    }

    @Test
    fun `preserves configured fallback agent id`() {
        assertThat(receiveAgentIds("prototype-agent")).containsExactly("prototype-agent")
    }

    @Test
    fun `omits agent id header when not configured or blank`() {
        listOf(null, "", "   ").forEach { agentId ->
            assertThat(receiveAgentIds(agentId)).isEmpty()
        }
    }

    private fun receiveAgentIds(agentId: String?): List<String> {
        val received = LinkedBlockingQueue<List<String>>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/da-ufs-sidecar/v1/getPersonInfoByRegionKind") { exchange ->
            received.add(exchange.requestHeaders["X-Agent-Id"]?.toList() ?: emptyList())
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
        }
        server.start()
        try {
            createContextRunner(server.address.port, agentId).run { context ->
                assertThat(context).hasNotFailed()
                context.getBean(HttpClientFactory::class.java).create("efs-adapter").use { client ->
                    runBlocking {
                        client.post(
                            "http://127.0.0.1:${server.address.port}/da-ufs-sidecar/v1/getPersonInfoByRegionKind"
                        )
                    }
                }
            }
            return checkNotNull(received.poll(5, TimeUnit.SECONDS)) { "HTTP request was not received" }
        } finally {
            server.stop(0)
        }
    }

    private fun createContextRunner(port: Int, agentId: String?): ApplicationContextRunner {
        val runner = ApplicationContextRunner()
            .withUserConfiguration(HttpClientFactoryConfiguration::class.java)
            .withPropertyValues(
                "http.clients.efs-adapter.base-url=http://127.0.0.1:$port",
                "http.clients.efs-adapter.connection-timeout=1000",
                "http.clients.efs-adapter.request-timeout=1000",
                "http.clients.efs-adapter.socket-timeout=1000",
                "http.clients.efs-adapter.pool.max-connections=1",
                "http.clients.efs-adapter.retry.max-retries=0",
                "http.clients.efs-adapter.retry.delay=1",
                "http.clients.efs-adapter.retry.max-delay=1",
                "http.clients.efs-adapter.retry.multiplier=1.0",
                "http.clients.efs-adapter.retry.status-codes[0]=502"
            )
        return if (agentId == null) runner else runner.withPropertyValues("aef.agent.agentId=$agentId")
    }
}
