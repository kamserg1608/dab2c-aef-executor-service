package ru.sbrf.dab2c.executor.it.tests.grpc.full

import GigaVoiceProtocol.GigaVoice
import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.ivr.proto.AudioSettings
import ru.sbrf.dab2c.executor.clients.ivr.proto.Context
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrRequest
import ru.sbrf.dab2c.executor.clients.ivr.proto.Settings
import ru.sbrf.dab2c.executor.it.support.WireMockResponses
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.withSession
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

/**
 * Integration tests for voice executor in non-proxy mode.
 * Uses WireMock to stub HTTP clients.
 */
class FullInteractionIntegrationTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should process settings through non-proxy flow with HTTP clients`() = runItTest {
        efsAdapterMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/configurator/rest-agent"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.EFS_ADAPTER_RESPONSE)
                )
        )

        efsAdapterMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/configurator/session"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.EFS_SESSION_CONFIG_RESPONSE)
                )
        )

        efsAdapterMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/session/readData"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.SDS_SESSION_READ_DATA_RESPONSE)
                )
        )

        gigaVoiceAgentMock.stubFor(
            WireMock.post(WireMock.urlEqualTo("/settings"))
                .willReturn(
                    WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.GIGA_VOICE_SETTINGS_RESPONSE)
                )
        )

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createContextRequest())
            session.sendRequest(
                IvrRequest.newBuilder()
                    .setSettings(
                        Settings.newBuilder()
                            .setVoiceCallId("test-call-123")
                            .setAudio(AudioSettings.getDefaultInstance())
                            .build()
                    )
                    .build()
            )

            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(createDefaultResponse())
            val response = session.awaitResponse()

            efsAdapterMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/configurator/rest-agent")))
            gigaVoiceAgentMock.verify(1, WireMock.postRequestedFor(WireMock.urlEqualTo("/settings")))

            Assertions.assertThat(response).isNotNull()
        }
    }

    private fun createContextRequest(): IvrRequest =
        IvrRequest.newBuilder()
            .setContext(
                Context.newBuilder()
                    .setContent("{}")
                    .build()
            )
            .build()

    private fun createDefaultResponse(): GigaVoice.GigaVoiceResponse =
        GigaVoice.GigaVoiceResponse.newBuilder()
            .setOutputTranscription(
                GigaVoice.OutputTranscription.newBuilder()
                    .setText("Response")
                    .build()
            )
            .build()
}
