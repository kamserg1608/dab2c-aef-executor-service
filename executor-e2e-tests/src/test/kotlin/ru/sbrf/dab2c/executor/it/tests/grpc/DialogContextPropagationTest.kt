package ru.sbrf.dab2c.executor.it.tests.grpc

import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.grpc.StatusException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.functionCallingResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.TestSessionScope
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentFunctionsForName
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentSettingsWithContext
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Verifies that the context an agent returns replaces the one held by the session, that a response
 * without the field leaves the context sent by IVR in place, and that a context chunk carrying
 * anything but a JSON object breaks the session before the agent is called.
 */
class DialogContextPropagationTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should pass the context returned by settings to functions and chain it further`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)
        gigaVoiceAgentMock.stubGigaAgentSettingsWithContext(SETTINGS_CONTEXT)
        gigaVoiceAgentMock.stubGigaAgentFunctionsForName(FIRST_FUNCTION, RESULT_CONTENT, FUNCTION_CONTEXT)
        gigaVoiceAgentMock.stubGigaAgentFunctionsForName(SECOND_FUNCTION, RESULT_CONTENT)

        callBothFunctions("dialog-context-chain")

        assertThat(settingsRequestContext()).isEqualTo(IVR_CONTEXT)
        assertThat(functionRequestContexts()).containsExactly(SETTINGS_CONTEXT, FUNCTION_CONTEXT)
    }

    @Test
    fun `should keep the context sent by IVR when the agent returns none`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)
        gigaVoiceAgentMock.stubGigaAgentSettingsWithContext()
        gigaVoiceAgentMock.stubGigaAgentFunctionsForName(FIRST_FUNCTION, RESULT_CONTENT)
        gigaVoiceAgentMock.stubGigaAgentFunctionsForName(SECOND_FUNCTION, RESULT_CONTENT)

        callBothFunctions("dialog-context-unchanged")

        assertThat(functionRequestContexts()).containsExactly(IVR_CONTEXT, IVR_CONTEXT)
    }

    @ParameterizedTest
    @ValueSource(strings = ["not-a-json", "[]", ""])
    fun `should break the session and never reach the agent on a context that is not a json object`(
        content: String
    ) = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val requests = Channel<GigaVoiceRequest>(Channel.UNLIMITED)
        val responses = mutableListOf<GigaVoiceResponse>()
        requests.send(contextRequest(content))
        requests.send(settingsRequest("dialog-context-invalid"))

        assertThrows<StatusException> {
            withTimeout(SESSION_TIMEOUT_MS) {
                testStub().gigaVoice(requests.consumeAsFlow()).collect { responses += it }
            }
        }

        assertThat(responses).isEmpty()
        gigaVoiceAgentMock.verify(0, postRequestedFor(urlEqualTo("/settings")))
    }

    private suspend fun callBothFunctions(voiceCallId: String) {
        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest(IVR_CONTEXT))
            session.sendRequest(settingsRequest(voiceCallId))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()

            session.sendRequest(audioRequest(speechStart = true))
            mock.awaitRequest { it.hasInput() }

            awaitFunctionResult(FIRST_FUNCTION)
            awaitFunctionResult(SECOND_FUNCTION)
        }
    }

    private suspend fun TestSessionScope.awaitFunctionResult(functionName: String) {
        mock.sendResponse(functionCallingResponse(functionName, """{"account_id": "12345"}"""))

        val result = mock.awaitRequest { it.hasFunctionResult() }
        assertThat(result.functionResult.functionName).isEqualTo(functionName)
    }

    private fun settingsRequestContext(): String =
        contextOf(gigaVoiceAgentMock.findAll(postRequestedFor(urlEqualTo("/settings"))).single().bodyAsString)

    private fun functionRequestContexts(): List<String> =
        gigaVoiceAgentMock.findAll(postRequestedFor(urlEqualTo("/functions"))).map { contextOf(it.bodyAsString) }

    private fun contextOf(body: String): String =
        ObjectMappers.MAPPER.readTree(body).get("context").toString()

    private companion object {
        const val IVR_CONTEXT = """{"ivr":true}"""
        const val SETTINGS_CONTEXT = """{"a":1}"""
        const val FUNCTION_CONTEXT = """{"b":2}"""
        const val RESULT_CONTENT = """{"balance": 1000}"""
        const val FIRST_FUNCTION = "get_account_balance"
        const val SECOND_FUNCTION = "check_transaction_status"
        const val SESSION_TIMEOUT_MS = 5000L
    }
}
