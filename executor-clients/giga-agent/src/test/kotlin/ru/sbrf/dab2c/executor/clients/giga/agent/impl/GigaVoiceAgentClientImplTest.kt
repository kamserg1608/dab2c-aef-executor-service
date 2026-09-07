package ru.sbrf.dab2c.executor.clients.giga.agent.impl

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.giga.agent.configuration.properties.GigaAgentPostProcessProperties
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceFunctionCallRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoicePostProcessRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceSettingsRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.RequestBuilderFixtures.agentConfiguration
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.RequestBuilderFixtures.daSessionInfo
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.RequestBuilderFixtures.objectNode
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCall
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.settings
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.HeadersElement
import ru.sbrf.dab2c.executor.library.context.SessionInfoElement
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Verifies that the context returned by the agent is carried into the client results.
 */
class GigaVoiceAgentClientImplTest {

    private val contextData = DialogContext(ObjectMappers.MAPPER.createObjectNode())

    private val functionCalling = functionCalling {
        functionCall = functionCall {
            name = "get_account_balance"
            arguments = """{"id":"1"}"""
        }
        timestamp = 1000L
    }

    @Test
    fun `should carry context from settings response`() = runTest {
        val client = clientReturning(settingsBody(""","context":{"a":1}"""))

        val result = inSessionContext { client.getSettings("conv-1", agentConfiguration, settings {}, contextData) }

        assertThat(result.context).isEqualTo(DialogContext(objectNode("""{"a":1}""")))
    }

    @Test
    fun `should leave context null when settings response has no context`() = runTest {
        val client = clientReturning(settingsBody())

        val result = inSessionContext { client.getSettings("conv-1", agentConfiguration, settings {}, contextData) }

        assertThat(result.context).isNull()
    }

    @Test
    fun `should carry an explicitly empty context as a value rather than as absence`() = runTest {
        val client = clientReturning(settingsBody(""","context":{}"""))

        val result = inSessionContext { client.getSettings("conv-1", agentConfiguration, settings {}, contextData) }

        assertThat(result.context).isEqualTo(DialogContext(ObjectMappers.MAPPER.createObjectNode()))
    }

    @Test
    fun `should carry context from function call response`() = runTest {
        val client = clientReturning("""{"function_result":{"content":"{}"},"context":{"b":2}}""")

        val result = inSessionContext {
            client.executeFunctionCall("conv-1", agentConfiguration, functionCalling, contextData)
        }

        assertThat(result.context).isEqualTo(DialogContext(objectNode("""{"b":2}""")))
    }

    @Test
    fun `should leave context null when function call response has no context`() = runTest {
        val client = clientReturning("""{"function_result":{"content":"{}"}}""")

        val result = inSessionContext {
            client.executeFunctionCall("conv-1", agentConfiguration, functionCalling, contextData)
        }

        assertThat(result.context).isNull()
    }

    private fun settingsBody(contextField: String = "") =
        """{"settings":{"audio":{},"voice_call_id":"conv-1"},"performers":{"functions":[]}$contextField}"""

    private fun clientReturning(body: String) = GigaVoiceAgentClientImpl(
        httpClient = HttpClient(
            StubHttpClientEngine { body }
        ) {
            install(ContentNegotiation) { jackson() }
        },
        objectMapper = ObjectMappers.MAPPER,
        baseUrl = "http://agent",
        settingsRequestBuilder = GigaVoiceSettingsRequestBuilder(),
        functionCallRequestBuilder = GigaVoiceFunctionCallRequestBuilder(),
        postProcessRequestBuilder = GigaVoicePostProcessRequestBuilder(),
        postProcessProperties = GigaAgentPostProcessProperties()
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
}
