package ru.sbrf.dab2c.executor.it.tests.grpc

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrRequest
import ru.sbrf.dab2c.executor.clients.ivr.proto.Settings
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

class GigaVoiceProxyIntegrationTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should proxy requests through Spring-managed service`() = runTest {
        // Given
        val requests = flow {
            repeat(3) { i ->
                emit(
                    IvrRequest.newBuilder()
                        .setSettings(
                            Settings.newBuilder()
                                .setVoiceCallId("spring-test-call-$i")
                                .build()
                        )
                        .build()
                )
            }
        }

        // When
        val responses = proxyStub().session(requests).toList()

        // Then
        assertThat(responses).hasSize(3)
        assertThat(responses.map { it.outputTranscription.text })
            .containsExactly("response-0", "response-1", "response-2")

        assertThat(mockGigaVoiceService.receivedRequests).hasSize(3)
        assertThat(mockGigaVoiceService.receivedRequests.map { it.settings.voiceCallId })
            .containsExactly("spring-test-call-0", "spring-test-call-1", "spring-test-call-2")
    }

    @Test
    fun `should handle multiple sequential streams`() = runTest {
        // First stream
        val firstRequests = flow {
            emit(
                IvrRequest.newBuilder()
                    .setSettings(Settings.newBuilder().setVoiceCallId("first-stream").build())
                    .build()
            )
        }
        val firstResponses = proxyStub().session(firstRequests).toList()
        assertThat(firstResponses).hasSize(1)

        // Second stream
        val secondRequests = flow {
            emit(
                IvrRequest.newBuilder()
                    .setSettings(Settings.newBuilder().setVoiceCallId("second-stream").build())
                    .build()
            )
        }
        val secondResponses = proxyStub().session(secondRequests).toList()
        assertThat(secondResponses).hasSize(1)
    }
}
