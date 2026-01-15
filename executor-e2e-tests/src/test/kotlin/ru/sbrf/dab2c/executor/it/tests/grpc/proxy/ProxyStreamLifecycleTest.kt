package ru.sbrf.dab2c.executor.it.tests.grpc.proxy

import GigaVoiceProtocol.GigaVoice.GigaVoiceResponse
import GigaVoiceProtocol.GigaVoice.OutputTranscription
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.ivr.proto.AudioContent
import ru.sbrf.dab2c.executor.clients.ivr.proto.ContentFromClient
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrRequest
import ru.sbrf.dab2c.executor.clients.ivr.proto.Settings
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.withSession
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

/**
 * Proxy Mode - Stream Lifecycle Tests.
 * Verifies stream ordering, session isolation, and edge cases.
 */
class ProxyStreamLifecycleTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should preserve message ordering in stream`() = runItTest {
        val messageCount = 10

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            repeat(messageCount) { i ->
                session.sendRequest(createSettingsRequest("order-test-$i"))
                val received = mock.awaitRequest { it.hasSettings() }
                assertThat(received.settings.voiceCallId).isEqualTo("order-test-$i")
                mock.sendResponse(createResponse("response-$i"))
                val response = session.awaitResponse()
                assertThat(response.outputTranscription.text).isEqualTo("response-$i")
            }
        }

        assertThat(mockGigaVoiceService.receivedRequests).hasSize(messageCount)
        mockGigaVoiceService.receivedRequests.forEachIndexed { index, request ->
            assertThat(request.settings.voiceCallId).isEqualTo("order-test-$index")
        }
    }

    @Test
    fun `should handle sequential streams independently`() = runItTest {
        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest("first-stream-1"))
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(createResponse("first-1"))
            session.awaitResponse()

            session.sendRequest(createSettingsRequest("first-stream-2"))
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(createResponse("first-2"))
            session.awaitResponse()
        }

        assertThat(mockGigaVoiceService.receivedRequests).hasSize(2)
        mockGigaVoiceService.reset()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            repeat(3) { i ->
                session.sendRequest(createSettingsRequest("second-stream-${i + 1}"))
                mock.awaitRequest { it.hasSettings() }
                mock.sendResponse(createResponse("response-$i"))
                val response = session.awaitResponse()
                assertThat(response.outputTranscription.text).isEqualTo("response-$i")
            }
        }

        assertThat(mockGigaVoiceService.receivedRequests).hasSize(3)
        assertThat(mockGigaVoiceService.receivedRequests.map { it.settings.voiceCallId })
            .containsExactly("second-stream-1", "second-stream-2", "second-stream-3")
    }

    @Test
    fun `should handle empty stream gracefully`() = runItTest {
        val requests = flow<IvrRequest> { }
        val responses = proxyStub().session(requests).toList()

        assertThat(responses).isEmpty()
        assertThat(mockGigaVoiceService.receivedRequests).isEmpty()
    }

    @Test
    fun `should handle large batch of messages`() = runItTest {
        val messageCount = 100

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            repeat(messageCount) { i ->
                session.sendRequest(createAudioRequest(speechStart = i == 0, speechEnd = i == messageCount - 1))
                mock.awaitRequest { it.hasInput() }
                mock.sendResponse(createResponse("response-$i"))
                session.awaitResponse()
            }
        }

        assertThat(mockGigaVoiceService.receivedRequests).hasSize(messageCount)
        assertThat(mockGigaVoiceService.receivedRequests.first().input.audioContent.speechStart).isTrue()
        assertThat(mockGigaVoiceService.receivedRequests.first().input.audioContent.speechEnd).isFalse()
        assertThat(mockGigaVoiceService.receivedRequests.last().input.audioContent.speechStart).isFalse()
        assertThat(mockGigaVoiceService.receivedRequests.last().input.audioContent.speechEnd).isTrue()
    }

    private fun createSettingsRequest(voiceCallId: String): IvrRequest =
        IvrRequest.newBuilder()
            .setSettings(
                Settings.newBuilder()
                    .setVoiceCallId(voiceCallId)
                    .build()
            )
            .build()

    private fun createAudioRequest(speechStart: Boolean = false, speechEnd: Boolean = false): IvrRequest =
        IvrRequest.newBuilder()
            .setInput(
                ContentFromClient.newBuilder()
                    .setAudioContent(
                        AudioContent.newBuilder()
                            .setSpeechStart(speechStart)
                            .setSpeechEnd(speechEnd)
                            .build()
                    )
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
