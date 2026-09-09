package ru.sbrf.dab2c.executor.clients.http.factory

import com.sun.net.httpserver.HttpServer
import io.ktor.client.request.header
import io.ktor.client.request.post
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.net.InetSocketAddress
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class HttpClientDefaultHeadersTest {

    @Test
    fun `settings request adds agent id and preserves trace id`() {
        val traceId = "2eeefd7d-4e31-4977-8276-51516592f59e"
        val received = LinkedBlockingQueue<Map<String, List<String>>>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/settings") { exchange ->
            received.add(
                exchange.requestHeaders.entries.associate { (name, values) ->
                    name.lowercase(Locale.ROOT) to values.toList()
                }
            )
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
        }
        server.start()
        try {
            contextRunner().run { context ->
                assertThat(context).hasNotFailed()
                context.getBean(HttpClientFactory::class.java).create("giga-agent").use { client ->
                    runBlocking {
                        client.post("http://127.0.0.1:${server.address.port}/settings") {
                            header("X-Trace-Id", traceId)
                        }
                    }
                }
            }
            val actual = checkNotNull(received.poll(5, TimeUnit.SECONDS)) { "HTTP request was not received" }
            assertThat(actual["x-agent-id"]).containsExactly("CI11659972")
            assertThat(actual["x-trace-id"]).containsExactly(traceId)
        } finally {
            server.stop(0)
        }
    }

    private fun contextRunner(): ApplicationContextRunner = ApplicationContextRunner()
        .withUserConfiguration(HttpClientFactoryConfiguration::class.java)
        .withPropertyValues(
            "aef.agent.agentId=CI11659972",
            "http.default-headers[X-Agent-Id]=\${aef.agent.agentId:}",
            "http.clients.giga-agent.base-url=http://127.0.0.1",
            "http.clients.giga-agent.connection-timeout=5000",
            "http.clients.giga-agent.request-timeout=5000",
            "http.clients.giga-agent.socket-timeout=5000",
            "http.clients.giga-agent.pool.max-connections=1",
            "http.clients.giga-agent.retry.max-retries=0",
            "http.clients.giga-agent.retry.delay=1",
            "http.clients.giga-agent.retry.max-delay=1",
            "http.clients.giga-agent.retry.multiplier=1.0",
            "http.clients.giga-agent.retry.status-codes[0]=502"
        )
}
