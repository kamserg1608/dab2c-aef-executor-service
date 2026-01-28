package ru.sbrf.dab2c.executor.it.tests.grpc.proxy

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

class ProxyModeIntegrationTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should proxy requests through Spring-managed service`() = runItTest {
        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            repeat(3) { i ->
                session.sendRequest(settingsRequest("spring-test-call-$i"))
                mock.awaitRequest { it.hasSettings() }
                mock.sendResponse(outputTranscriptionResponse("response-$i"))
                val response = session.awaitResponse()
                assertThat(response.outputTranscription.text).isEqualTo("response-$i")
            }
        }

        assertThat(mockGigaVoiceService.receivedRequests).hasSize(3)
        assertThat(mockGigaVoiceService.receivedRequests.map { it.settings.voiceCallId })
            .containsExactly("spring-test-call-0", "spring-test-call-1", "spring-test-call-2")
    }

    @Test
    fun `should handle multiple sequential streams`() = runItTest {
        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(settingsRequest("first-stream"))
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(outputTranscriptionResponse("first"))
            session.awaitResponse()
        }

        mockGigaVoiceService.reset()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(settingsRequest("second-stream"))
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(outputTranscriptionResponse("second"))
            session.awaitResponse()
        }
    }
}
