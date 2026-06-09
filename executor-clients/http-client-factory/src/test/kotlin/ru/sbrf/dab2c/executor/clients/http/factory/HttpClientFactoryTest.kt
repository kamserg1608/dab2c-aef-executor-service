package ru.sbrf.dab2c.executor.clients.http.factory

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test

class HttpClientFactoryTest {

    private val factory = HttpClientFactory(
        HttpClientsProperties(
            mapOf(
                "known" to HttpClientProperties(
                    baseUrl = "http://localhost:8080",
                    connectionTimeout = 1000,
                    requestTimeout = 1000,
                    socketTimeout = 1000,
                    pool = ConnectionPoolProperties(maxConnections = 1),
                    retry = RetryProperties(
                        maxRetries = 1,
                        delay = 1,
                        maxDelay = 1,
                        multiplier = 1.0,
                        statusCodes = listOf(502)
                    )
                )
            )
        )
    )

    @Test
    fun `creates client for configured name`() {
        factory.create("known").use { client ->
            assertThat(client).isNotNull()
        }
    }

    @Test
    fun `fails for unknown client name`() {
        assertThatIllegalStateException()
            .isThrownBy { factory.create("unknown") }
            .withMessageContaining("unknown")
    }
}
