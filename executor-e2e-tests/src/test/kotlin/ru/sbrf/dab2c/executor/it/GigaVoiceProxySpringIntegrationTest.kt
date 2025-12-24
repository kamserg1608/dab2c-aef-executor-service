package ru.sbrf.dab2c.executor.it

import GigaVoiceProtocol.GigaVoice.GigaVoiceRequest
import GigaVoiceProtocol.GigaVoice.Settings
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GigaVoiceProxySpringIntegrationTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should proxy requests through Spring-managed service`() = runTest {
        // Given
        val requests = flow {
            repeat(3) { i ->
                emit(
                    GigaVoiceRequest.newBuilder()
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
        val responses = clientStub.gigaVoice(requests).toList()

        // Then
        assertThat(responses).hasSize(3)
        assertThat(responses.map { it.outputTranscription.text })
            .containsExactly("response-0", "response-1", "response-2")

        assertThat(mockDownstreamService.receivedRequests).hasSize(3)
        assertThat(mockDownstreamService.receivedRequests.map { it.settings.voiceCallId })
            .containsExactly("spring-test-call-0", "spring-test-call-1", "spring-test-call-2")
    }

    @Test
    fun `should handle multiple sequential streams`() = runTest {
        // First stream
        val firstRequests = flow {
            emit(
                GigaVoiceRequest.newBuilder()
                    .setSettings(Settings.newBuilder().setVoiceCallId("first-stream").build())
                    .build()
            )
        }
        val firstResponses = clientStub.gigaVoice(firstRequests).toList()
        assertThat(firstResponses).hasSize(1)

        // Second stream
        val secondRequests = flow {
            emit(
                GigaVoiceRequest.newBuilder()
                    .setSettings(Settings.newBuilder().setVoiceCallId("second-stream").build())
                    .build()
            )
        }
        val secondResponses = clientStub.gigaVoice(secondRequests).toList()
        assertThat(secondResponses).hasSize(1)
    }
}
