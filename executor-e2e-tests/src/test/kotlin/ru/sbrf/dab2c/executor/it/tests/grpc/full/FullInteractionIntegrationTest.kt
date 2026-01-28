package ru.sbrf.dab2c.executor.it.tests.grpc.full

import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupFullModeStubs
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

/**
 * Integration tests for voice executor in non-proxy mode.
 * Uses WireMock to stub HTTP clients.
 */
class FullInteractionIntegrationTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should process settings through non-proxy flow with HTTP clients`() = runItTest {
        setupFullModeStubs(efsAdapterMock, gigaVoiceAgentMock)

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("test-call-123"))

            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse())
            val response = session.awaitResponse()

            efsAdapterMock.verify(1, postRequestedFor(urlEqualTo("/configurator/rest-agent")))
            gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/settings")))

            assertThat(response).isNotNull()
        }
    }
}
