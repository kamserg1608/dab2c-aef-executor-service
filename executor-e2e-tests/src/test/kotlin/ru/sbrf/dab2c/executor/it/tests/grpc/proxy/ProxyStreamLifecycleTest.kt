package ru.sbrf.dab2c.executor.it.tests.grpc.proxy

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.ivr.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
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
                session.sendRequest(settingsRequest("order-test-$i"))
                val received = mock.awaitRequest { it.hasSettings() }
                assertThat(received.settings.voiceCallId).isEqualTo("order-test-$i")
                mock.sendResponse(outputTranscriptionResponse("response-$i"))
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
            session.sendRequest(settingsRequest("first-stream-1"))
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(outputTranscriptionResponse("first-1"))
            session.awaitResponse()

            session.sendRequest(settingsRequest("first-stream-2"))
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(outputTranscriptionResponse("first-2"))
            session.awaitResponse()
        }

        assertThat(mockGigaVoiceService.receivedRequests).hasSize(2)
        mockGigaVoiceService.reset()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            repeat(3) { i ->
                session.sendRequest(settingsRequest("second-stream-${i + 1}"))
                mock.awaitRequest { it.hasSettings() }
                mock.sendResponse(outputTranscriptionResponse("response-$i"))
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
        val requests = flow<GigaVoiceRequest> { }
        val responses = proxyStub().gigaVoice(requests).toList()

        assertThat(responses).isEmpty()
        assertThat(mockGigaVoiceService.receivedRequests).isEmpty()
    }

    @Test
    fun `should handle large batch of messages`() = runItTest {
        val messageCount = 100

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            repeat(messageCount) { i ->
                session.sendRequest(audioRequest(speechStart = i == 0, speechEnd = i == messageCount - 1))
                mock.awaitRequest { it.hasInput() }
                mock.sendResponse(outputTranscriptionResponse("response-$i"))
                session.awaitResponse()
            }
        }

        assertThat(mockGigaVoiceService.receivedRequests).hasSize(messageCount)
        assertThat(mockGigaVoiceService.receivedRequests.first().input.audioContent.speechStart).isTrue()
        assertThat(mockGigaVoiceService.receivedRequests.first().input.audioContent.speechEnd).isFalse()
        assertThat(mockGigaVoiceService.receivedRequests.last().input.audioContent.speechStart).isFalse()
        assertThat(mockGigaVoiceService.receivedRequests.last().input.audioContent.speechEnd).isTrue()
    }
}
