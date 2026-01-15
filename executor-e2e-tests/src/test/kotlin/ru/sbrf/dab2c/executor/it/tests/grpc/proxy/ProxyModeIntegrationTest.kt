package ru.sbrf.dab2c.executor.it.tests.grpc.proxy

import GigaVoiceProtocol.GigaVoice.GigaVoiceResponse
import GigaVoiceProtocol.GigaVoice.OutputTranscription
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrRequest
import ru.sbrf.dab2c.executor.clients.ivr.proto.Settings
import ru.sbrf.dab2c.executor.it.support.withSession
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

class ProxyModeIntegrationTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should proxy requests through Spring-managed service`() = runBlocking {
        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            repeat(3) { i ->
                session.sendRequest(createSettingsRequest("spring-test-call-$i"))
                mock.awaitRequest { it.hasSettings() }
                mock.sendResponse(createResponse("response-$i"))
                val response = session.awaitResponse()
                assertThat(response.outputTranscription.text).isEqualTo("response-$i")
            }
        }

        assertThat(mockGigaVoiceService.receivedRequests).hasSize(3)
        assertThat(mockGigaVoiceService.receivedRequests.map { it.settings.voiceCallId })
            .containsExactly("spring-test-call-0", "spring-test-call-1", "spring-test-call-2")
    }

    @Test
    fun `should handle multiple sequential streams`() = runBlocking {
        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest("first-stream"))
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(createResponse("first"))
            session.awaitResponse()
        }

        mockGigaVoiceService.reset()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest("second-stream"))
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(createResponse("second"))
            session.awaitResponse()
        }
    }

    private fun createSettingsRequest(voiceCallId: String): IvrRequest =
        IvrRequest.newBuilder()
            .setSettings(
                Settings.newBuilder()
                    .setVoiceCallId(voiceCallId)
                    .build()
            )
            .build()

    private fun createResponse(text: String): GigaVoiceResponse =
        GigaVoiceResponse.newBuilder()
            .setOutputTranscription(
                OutputTranscription.newBuilder()
                    .setText(text)
                    .build()
            )
            .build()
}
