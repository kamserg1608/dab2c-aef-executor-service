package ru.sbrf.dab2c.executor.it.tests.grpc

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.wiremock.spring.InjectWireMock
import ru.sbrf.dab2c.executor.clients.ivr.proto.AudioSettings
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrRequest
import ru.sbrf.dab2c.executor.clients.ivr.proto.Settings
import ru.sbrf.dab2c.executor.it.support.WireMockResponses
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import kotlin.time.Duration.Companion.seconds

/**
 * Integration tests for voice executor in non-proxy mode.
 * Uses WireMock to stub HTTP clients (ConfiguratorClient, GigaVoiceAgentClient).
 */
class VoiceExecutorNonProxyIntegrationTest : BaseGigaVoiceIntegrationTest() {

    @InjectWireMock("gigaVoiceAgent")
    lateinit var gigaVoiceAgentMock: WireMockServer

    @InjectWireMock("efsAdapter")
    lateinit var efsAdapterMock: WireMockServer

    @BeforeEach
    fun resetWireMocks() {
        gigaVoiceAgentMock.resetAll()
        efsAdapterMock.resetAll()
    }

    @Test
    fun `should process settings through non-proxy flow with HTTP clients`() = runTest(timeout = 30.seconds) {
        // Setup EFS adapter stub
        efsAdapterMock.stubFor(
            post(urlEqualTo("/configurator/rest-agent"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.EFS_ADAPTER_RESPONSE)
                )
        )

        // Setup GigaVoice agent stub
        gigaVoiceAgentMock.stubFor(
            post(urlEqualTo("/settings"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.GIGA_VOICE_SETTINGS_RESPONSE)
                )
        )

        // Create request
        val requests = flow {
            emit(
                IvrRequest.newBuilder()
                    .setSettings(
                        Settings.newBuilder()
                            .setVoiceCallId("test-call-123")
                            .setAudio(AudioSettings.getDefaultInstance())
                            .build()
                    )
                    .build()
            )
        }

        // Execute request with non-proxy stub (proxy=false header + session/token)
        val firstResponse = nonProxyStub().session(requests).first()

        // Give time for async HTTP calls to complete
        delay(500)

        // Verify HTTP calls were made to external services
        efsAdapterMock.verify(1, postRequestedFor(urlEqualTo("/configurator/rest-agent")))
        gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/settings")))

        // Verify we got a response
        assertThat(firstResponse).isNotNull()
    }
}
