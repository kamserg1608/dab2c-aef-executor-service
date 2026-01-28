package ru.sbrf.dab2c.executor.it.tests.grpc.full

import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.functionResultRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.textForSynthesisRequest
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupFullModeStubs
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubEfsRestAgent
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentSettingsWithDelay
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import kotlin.time.Duration.Companion.milliseconds

class ChunkSkippingBehaviorTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should skip audio chunks sent before context`() = runItTest {
        setupFullModeStubs(efsAdapterMock, gigaVoiceAgentMock)

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(audioRequest(speechStart = true))
            delay(100.milliseconds)

            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("test"))
            mock.awaitRequest { it.hasSettings() }

            val audioRequests = mock.receivedRequests.filter {
                it.hasInput() && it.input.hasAudioContent()
            }
            assertThat(audioRequests).isEmpty()
        }
    }

    @Test
    fun `should skip function result sent before context`() = runItTest {
        setupFullModeStubs(efsAdapterMock, gigaVoiceAgentMock)

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(functionResultRequest("test_function", """{"result": "value"}"""))
            delay(100.milliseconds)

            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("test"))
            mock.awaitRequest { it.hasSettings() }

            val functionResultRequests = mock.receivedRequests.filter { it.hasFunctionResult() }
            assertThat(functionResultRequests).isEmpty()
        }
    }

    @Test
    fun `should skip text for synthesis sent before context`() = runItTest {
        setupFullModeStubs(efsAdapterMock, gigaVoiceAgentMock)

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(textForSynthesisRequest("Hello, how can I help?"))
            delay(100.milliseconds)

            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("test"))
            mock.awaitRequest { it.hasSettings() }

            val synthesisRequests = mock.receivedRequests.filter {
                it.hasInput() && it.input.hasContentForSynthesis()
            }
            assertThat(synthesisRequests).isEmpty()
        }
    }

    @Test
    fun `should skip audio sent while settings calculation is in progress`() = runItTest {
        efsAdapterMock.stubEfsRestAgent()
        gigaVoiceAgentMock.stubGigaAgentSettingsWithDelay(SETTINGS_DELAY_MS)

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("test"))

            delay(50.milliseconds)
            session.sendRequest(audioRequest(speechStart = true))

            mock.awaitRequest { it.hasSettings() }

            val audioRequests = mock.receivedRequests.filter {
                it.hasInput() && it.input.hasAudioContent()
            }
            assertThat(audioRequests).isEmpty()
        }
    }

    @Test
    fun `should skip multiple chunk types sent before context`() = runItTest {
        setupFullModeStubs(efsAdapterMock, gigaVoiceAgentMock)

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(audioRequest(speechStart = true))
            session.sendRequest(functionResultRequest("test_function", """{"result": "value"}"""))
            session.sendRequest(textForSynthesisRequest("Hello"))
            delay(100.milliseconds)

            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("test"))
            mock.awaitRequest { it.hasSettings() }

            val audioRequests = mock.receivedRequests.filter {
                it.hasInput() && it.input.hasAudioContent()
            }
            val functionResultRequests = mock.receivedRequests.filter { it.hasFunctionResult() }
            val synthesisRequests = mock.receivedRequests.filter {
                it.hasInput() && it.input.hasContentForSynthesis()
            }

            assertThat(audioRequests).isEmpty()
            assertThat(functionResultRequests).isEmpty()
            assertThat(synthesisRequests).isEmpty()
        }
    }

    companion object {
        private const val SETTINGS_DELAY_MS = 500
    }
}
