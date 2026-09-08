package ru.sbrf.dab2c.executor.clients.giga.agent.impl

import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.giga.agent.configuration.properties.GigaAgentPostProcessProperties
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceFunctionCallRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoicePostProcessRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceSettingsRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.RequestBuilderFixtures.agentConfiguration
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.RequestBuilderFixtures.daSessionInfo
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.settings
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.HeadersElement
import ru.sbrf.dab2c.executor.library.context.SessionInfoElement
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Verifies the analytics mapping of the post-processing response and the per-request timeout
 * that overrides the shared giga-agent client settings, leaving the request timeout as the only
 * deadline so a slow handle is never retried.
 */
class GigaVoiceAgentClientImplPostProcessTest {

    private val contextData = DialogContext(ObjectMappers.MAPPER.createObjectNode())
    private val capturedRequests = mutableListOf<HttpRequestData>()

    @Test
    fun `should map agent analytics from the response`() = runTest {
        val client = clientReturning(
            """{"agent_analytics":[{"data_version":"v1","data":{"a":1}}]}"""
        )

        val result = inSessionContext { client.postProcess(CONVERSATION_ID, agentConfiguration, contextData) }

        assertThat(result.analytics).singleElement()
            .satisfies({ assertThat(it.dataVersion).isEqualTo("v1") }, { assertThat(it.data).isEqualTo("""{"a":1}""") })
    }

    @Test
    fun `should map a response without agent analytics into an empty list`() = runTest {
        val client = clientReturning("{}")

        val result = inSessionContext { client.postProcess(CONVERSATION_ID, agentConfiguration, contextData) }

        assertThat(result.analytics).isEmpty()
    }

    @Test
    fun `should override the request timeout and disable the socket timeout for postprocess`() = runTest {
        val client = clientReturning("{}")

        inSessionContext {
            client.getSettings(CONVERSATION_ID, agentConfiguration, settings {}, contextData)
            client.postProcess(CONVERSATION_ID, agentConfiguration, contextData)
        }

        val settingsTimeout = capturedRequests.first().getCapabilityOrNull(HttpTimeoutCapability)
        val postProcessTimeout = capturedRequests.last().getCapabilityOrNull(HttpTimeoutCapability)

        assertThat(settingsTimeout?.requestTimeoutMillis).isEqualTo(CLIENT_TIMEOUT_MS)
        assertThat(settingsTimeout?.socketTimeoutMillis).isEqualTo(CLIENT_TIMEOUT_MS)
        assertThat(postProcessTimeout?.requestTimeoutMillis).isEqualTo(POSTPROCESS_TIMEOUT_MS)
        assertThat(postProcessTimeout?.socketTimeoutMillis).isEqualTo(HttpTimeoutConfig.INFINITE_TIMEOUT_MS)
    }

    @Test
    fun `should send a single request and fail with a request timeout when the handle never answers`() {
        val client = clientFailingOnTheEarliestDeadline()

        val thrown = catchThrowable {
            runBlocking { inSessionContext { client.postProcess(CONVERSATION_ID, agentConfiguration, contextData) } }
        }

        assertThat(capturedRequests).hasSize(1)
        assertThat(thrown).isInstanceOf(HttpRequestTimeoutException::class.java)
    }

    private fun clientFailingOnTheEarliestDeadline() = GigaVoiceAgentClientImpl(
        httpClient = HttpClient(
            StubHttpClientEngine { request ->
                capturedRequests += request
                val timeouts = request.getCapabilityOrNull(HttpTimeoutCapability)
                val socketDeadline = timeouts?.socketTimeoutMillis ?: SHORT_CLIENT_TIMEOUT_MS
                val requestDeadline = timeouts?.requestTimeoutMillis ?: SHORT_CLIENT_TIMEOUT_MS
                if (socketDeadline <= requestDeadline) throw SocketTimeoutException("socket deadline reached")
                delay(HttpTimeoutConfig.INFINITE_TIMEOUT_MS)
                error("unreachable")
            }
        ) {
            install(ContentNegotiation) { jackson() }
            install(HttpTimeout) {
                requestTimeoutMillis = SHORT_CLIENT_TIMEOUT_MS
                socketTimeoutMillis = SHORT_CLIENT_TIMEOUT_MS
            }
            install(HttpRequestRetry) {
                maxRetries = MAX_RETRIES
                retryOnExceptionIf { _, cause -> cause is SocketTimeoutException }
                delayMillis { 0 }
            }
        },
        objectMapper = ObjectMappers.MAPPER,
        baseUrl = "http://agent",
        settingsRequestBuilder = GigaVoiceSettingsRequestBuilder(),
        functionCallRequestBuilder = GigaVoiceFunctionCallRequestBuilder(),
        postProcessRequestBuilder = GigaVoicePostProcessRequestBuilder(),
        postProcessProperties = GigaAgentPostProcessProperties(timeout = SHORT_POSTPROCESS_TIMEOUT_MS)
    )

    private fun clientReturning(body: String) = GigaVoiceAgentClientImpl(
        httpClient = HttpClient(
            StubHttpClientEngine { request ->
                capturedRequests += request
                if (request.url.encodedPath == "/settings") SETTINGS_BODY else body
            }
        ) {
            install(ContentNegotiation) { jackson() }
            install(HttpTimeout) {
                requestTimeoutMillis = CLIENT_TIMEOUT_MS
                socketTimeoutMillis = CLIENT_TIMEOUT_MS
            }
        },
        objectMapper = ObjectMappers.MAPPER,
        baseUrl = "http://agent",
        settingsRequestBuilder = GigaVoiceSettingsRequestBuilder(),
        functionCallRequestBuilder = GigaVoiceFunctionCallRequestBuilder(),
        postProcessRequestBuilder = GigaVoicePostProcessRequestBuilder(),
        postProcessProperties = GigaAgentPostProcessProperties(timeout = POSTPROCESS_TIMEOUT_MS)
    )

    private suspend fun <T> inSessionContext(block: suspend () -> T): T = withContext(
        HeadersElement(
            Headers(
                mapOf(
                    "x-session" to "test-session",
                    "x-token" to "test-token",
                    "x-eduid" to "test-edu-id",
                    "x-channel" to "test-channel",
                    "x-platform" to "test-platform"
                )
            )
        ) + SessionInfoElement(daSessionInfo)
    ) { block() }

    private companion object {
        const val CONVERSATION_ID = "conv-1"
        const val CLIENT_TIMEOUT_MS = 30_000L
        const val POSTPROCESS_TIMEOUT_MS = 60_000L
        const val SHORT_CLIENT_TIMEOUT_MS = 100L
        const val SHORT_POSTPROCESS_TIMEOUT_MS = 400L
        const val MAX_RETRIES = 3
        const val SETTINGS_BODY =
            """{"settings":{"audio":{},"voice_call_id":"conv-1"},"performers":{"functions":[]}}"""
    }
}
