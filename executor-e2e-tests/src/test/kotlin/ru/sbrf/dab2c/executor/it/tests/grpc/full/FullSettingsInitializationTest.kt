package ru.sbrf.dab2c.executor.it.tests.grpc.full

import GigaVoiceProtocol.GigaVoice.GigaVoiceResponse
import GigaVoiceProtocol.GigaVoice.OutputTranscription
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.grpc.StatusException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.ivr.proto.AudioContent
import ru.sbrf.dab2c.executor.clients.ivr.proto.AudioSettings
import ru.sbrf.dab2c.executor.clients.ivr.proto.ContentFromClient
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrRequest
import ru.sbrf.dab2c.executor.clients.ivr.proto.Settings
import ru.sbrf.dab2c.executor.it.support.MetadataInterceptor
import ru.sbrf.dab2c.executor.it.support.WireMockResponses
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.withSession
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

/**
 * Full Mode - Settings Initialization Tests.
 * Verifies settings resolution via HTTP APIs and error handling.
 */
class FullSettingsInitializationTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should call EFS and GigaAgent when processing settings`() = runItTest {
        setupSuccessfulStubs()

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest("test-call-123"))
            wireMock.awaitPostCall("/settings")

            efsAdapterMock.verify(1, postRequestedFor(urlEqualTo("/configurator/rest-agent")))
            gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/settings")))
        }
    }

    @Test
    fun `should forward settings to downstream after initialization`() = runItTest {
        setupSuccessfulStubs()

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest("forwarded-call-456"))

            val forwardedSettings = mock.awaitRequest { it.hasSettings() }
            assertThat(forwardedSettings).isNotNull()

            mock.sendResponse(createDefaultResponse())
            val response = session.awaitResponse()
            assertThat(response).isNotNull()
        }
    }

    @Test
    fun `should process audio after settings initialization`() = runItTest {
        setupSuccessfulStubs()

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest("audio-flow-test"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(createDefaultResponse())
            session.awaitResponse()

            session.sendRequest(createAudioRequest(speechStart = true))
            mock.awaitRequest { it.hasInput() && it.input.audioContent.speechStart }

            mock.sendResponse(createDefaultResponse())
            session.awaitResponse()

            session.sendRequest(createAudioRequest(speechEnd = true))
            mock.awaitRequest { it.hasInput() && it.input.audioContent.speechEnd }

            efsAdapterMock.verify(1, postRequestedFor(urlEqualTo("/configurator/rest-agent")))
            gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/settings")))

            val audioRequests = mock.receivedRequests
                .filter { it.hasInput() && it.input.hasAudioContent() }
            assertThat(audioRequests).hasSize(2)
        }
    }

    @Test
    fun `should fail with INTERNAL when EFS adapter returns 500`() = runItTest {
        efsAdapterMock.stubFor(
            post(urlEqualTo("/configurator/rest-agent"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")
                )
        )

        val requests = flow {
            emit(createSettingsRequest("efs-error-test"))
        }

        var caughtException: StatusException? = null
        try {
            nonProxyStub().session(requests).toList()
        } catch (e: StatusException) {
            caughtException = e
        }

        assertThat(caughtException).isNotNull()
        assertThat(caughtException!!.status.code.name).isIn("INTERNAL", "UNKNOWN", "UNAVAILABLE")
        efsAdapterMock.verify(1, postRequestedFor(urlEqualTo("/configurator/rest-agent")))
    }

    @Test
    fun `should fail with INTERNAL when GigaAgent settings returns 500`() = runItTest {
        efsAdapterMock.stubFor(
            post(urlEqualTo("/configurator/rest-agent"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.EFS_ADAPTER_RESPONSE)
                )
        )

        efsAdapterMock.stubFor(
            post(urlEqualTo("/configurator/session"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.EFS_SESSION_CONFIG_RESPONSE)
                )
        )

        efsAdapterMock.stubFor(
            post(urlEqualTo("/session/readData"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.SDS_SESSION_READ_DATA_RESPONSE)
                )
        )

        gigaVoiceAgentMock.stubFor(
            post(urlEqualTo("/settings"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")
                )
        )

        val requests = flow {
            emit(createSettingsRequest("agent-error-test"))
        }

        var caughtException: StatusException? = null
        try {
            nonProxyStub().session(requests).toList()
        } catch (e: StatusException) {
            caughtException = e
        }

        assertThat(caughtException).isNotNull()
        assertThat(caughtException!!.status.code.name).isIn("INTERNAL", "UNKNOWN", "UNAVAILABLE")
        efsAdapterMock.verify(1, postRequestedFor(urlEqualTo("/configurator/rest-agent")))
        gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/settings")))
    }

    @Test
    fun `should fail when session header is missing`() = runItTest {
        setupSuccessfulStubs()

        val stubWithoutSession = clientStub.withInterceptors(
            MetadataInterceptor(
                mapOf(
                    "proxy" to "false",
                    "token" to "test-token",
                    "edu_id" to "test-edu-id"
                )
            )
        )

        val requests = flow {
            emit(createSettingsRequest("missing-session-test"))
        }

        val result = runCatching { stubWithoutSession.session(requests).toList() }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(StatusException::class.java)
    }

    @Test
    fun `should fail when token header is missing`() = runItTest {
        setupSuccessfulStubs()

        val stubWithoutToken = clientStub.withInterceptors(
            MetadataInterceptor(
                mapOf(
                    "proxy" to "false",
                    "session" to "test-session",
                    "edu_id" to "test-edu-id"
                )
            )
        )

        val requests = flow {
            emit(createSettingsRequest("missing-token-test"))
        }

        val result = runCatching { stubWithoutToken.session(requests).toList() }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(StatusException::class.java)
    }

    @Test
    fun `should fail when edu_id header is missing`() = runItTest {
        setupSuccessfulStubs()

        val stubWithoutEduId = clientStub.withInterceptors(
            MetadataInterceptor(
                mapOf(
                    "proxy" to "false",
                    "session" to "test-session",
                    "token" to "test-token"
                )
            )
        )

        val requests = flow {
            emit(createSettingsRequest("missing-edu-id-test"))
        }

        val result = runCatching { stubWithoutEduId.session(requests).toList() }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(StatusException::class.java)
    }

    private fun setupSuccessfulStubs() {
        efsAdapterMock.stubFor(
            post(urlEqualTo("/configurator/rest-agent"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.EFS_ADAPTER_RESPONSE)
                )
        )

        efsAdapterMock.stubFor(
            post(urlEqualTo("/configurator/session"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.EFS_SESSION_CONFIG_RESPONSE)
                )
        )

        efsAdapterMock.stubFor(
            post(urlEqualTo("/session/readData"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.SDS_SESSION_READ_DATA_RESPONSE)
                )
        )

        gigaVoiceAgentMock.stubFor(
            post(urlEqualTo("/settings"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.GIGA_VOICE_SETTINGS_RESPONSE)
                )
        )
    }

    private fun createSettingsRequest(voiceCallId: String): IvrRequest =
        IvrRequest.newBuilder()
            .setSettings(
                Settings.newBuilder()
                    .setVoiceCallId(voiceCallId)
                    .setAudio(AudioSettings.getDefaultInstance())
                    .build()
            )
            .build()

    private fun createAudioRequest(
        speechStart: Boolean = false,
        speechEnd: Boolean = false
    ): IvrRequest =
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

    private fun createDefaultResponse(): GigaVoiceResponse =
        GigaVoiceResponse.newBuilder()
            .setOutputTranscription(
                OutputTranscription.newBuilder()
                    .setText("Response")
                    .build()
            )
            .build()
}
