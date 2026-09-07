package ru.sbrf.dab2c.executor.it.tests.grpc

import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.functionCallingResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockAwaiter
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubsWithFunctionMatchIag
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentSettingsWithContext
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Verifies what an IAG function sees in its context message: the context accumulated by the
 * session rather than the one IVR sent, and an empty object where nothing was sent at all.
 */
class IagDialogContextTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should send the context returned by settings to IAG`() = runItTest {
        setupIagStubs()
        gigaVoiceAgentMock.stubGigaAgentSettingsWithContext(SETTINGS_CONTEXT)

        callIagFunction("iag-context-updated", IVR_CONTEXT)

        assertThat(iagContextMessage()).isEqualTo(SETTINGS_CONTEXT)
    }

    @Test
    fun `should send an empty json object to IAG when nothing updated the context`() = runItTest {
        setupIagStubs()
        gigaVoiceAgentMock.stubGigaAgentSettingsWithContext()

        callIagFunction("iag-context-empty", "{}")

        assertThat(iagContextMessage()).isEqualTo("{}")
    }

    private fun setupIagStubs() = setupStubsWithFunctionMatchIag(
        efsAdapterMock,
        configuratorMock,
        gigaVoiceAgentMock,
        iagMock,
        configuratorEnabled = true
    )

    private suspend fun callIagFunction(voiceCallId: String, ivrContext: String) {
        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest(ivrContext))
            session.sendRequest(settingsRequest(voiceCallId))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()

            session.sendRequest(audioRequest(speechStart = true))
            mock.awaitRequest { it.hasInput() }

            mock.sendResponse(functionCallingResponse(IAG_FUNCTION, """{"city": "Moscow"}"""))
            WireMockAwaiter(iagMock).awaitPostCall("/bh")
        }
    }

    private fun iagContextMessage(): String {
        val body = iagMock.findAll(postRequestedFor(urlEqualTo("/bh"))).single().bodyAsString
        val content = ObjectMappers.MAPPER.readTree(body).at("/message/content/message/0")

        assertThat(content.get("type").asText()).isEqualTo("context")

        return content.get("value").asText()
    }

    private companion object {
        const val IVR_CONTEXT = """{"ivr":true}"""
        const val SETTINGS_CONTEXT = """{"a":1}"""
        const val IAG_FUNCTION = "find_bank_office_iag"
    }
}
