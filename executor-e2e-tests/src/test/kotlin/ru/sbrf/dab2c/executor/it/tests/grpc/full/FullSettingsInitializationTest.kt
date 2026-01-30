package ru.sbrf.dab2c.executor.it.tests.grpc.full

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.grpc.StatusException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.grpc.MetadataInterceptor
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockResponses
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupFullModeStubs
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

/**
 * Full Mode - Settings Initialization Tests.
 * Verifies settings resolution via HTTP APIs and error handling.
 */
class FullSettingsInitializationTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should call EFS and GigaAgent when processing settings`() = runItTest {
        setupFullModeStubs(efsAdapterMock, gigaVoiceAgentMock)

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("test-call-123"))
            wireMock.awaitPostCall("/settings")

            efsAdapterMock.verify(1, postRequestedFor(urlEqualTo("/configurator/rest-agent")))
            gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/settings")))
        }
    }

    @Test
    fun `should forward settings to downstream after initialization`() = runItTest {
        setupFullModeStubs(efsAdapterMock, gigaVoiceAgentMock)

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("forwarded-call-456"))

            val forwardedSettings = mock.awaitRequest { it.hasSettings() }
            assertThat(forwardedSettings).isNotNull()

            mock.sendResponse(outputTranscriptionResponse())
            val response = session.awaitResponse()
            assertThat(response).isNotNull()
        }
    }

    @Test
    fun `should process audio after settings initialization`() = runItTest {
        setupFullModeStubs(efsAdapterMock, gigaVoiceAgentMock)

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("audio-flow-test"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()

            session.sendRequest(audioRequest(speechStart = true))
            mock.awaitRequest { it.hasInput() && it.input.audioContent.speechStart }

            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()

            session.sendRequest(audioRequest(speechEnd = true))
            mock.awaitRequest { it.hasInput() && it.input.audioContent.speechEnd }

            efsAdapterMock.verify(1, postRequestedFor(urlEqualTo("/configurator/rest-agent")))
            gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/settings")))

            val audioRequests = mock.receivedRequests
                .filter { it.hasInput() && it.input.hasAudioContent() }
            assertThat(audioRequests).hasSize(2)
        }
    }

    @Test
    fun `should send error response when EFS adapter returns 500`() = runItTest {
        efsAdapterMock.stubFor(
            post(urlEqualTo("/configurator/rest-agent"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")
                )
        )

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("efs-error-test"))

            val errorResponse = session.awaitResponse { it.hasError() }
            assertThat(errorResponse.error.status).isEqualTo(1501)
            assertThat(errorResponse.error.message).isNotBlank()
        }

        efsAdapterMock.verify(1, postRequestedFor(urlEqualTo("/configurator/rest-agent")))
    }

    @Test
    fun `should send error response when GigaAgent settings returns 500`() = runItTest {
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

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("agent-error-test"))

            val errorResponse = session.awaitResponse { it.hasError() }
            assertThat(errorResponse.error.status).isEqualTo(1501)
            assertThat(errorResponse.error.message).isNotBlank()
        }

        efsAdapterMock.verify(1, postRequestedFor(urlEqualTo("/configurator/rest-agent")))
        gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/settings")))
    }

    @Test
    fun `should fail when session header is missing`() = runItTest {
        setupFullModeStubs(efsAdapterMock, gigaVoiceAgentMock)

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
            emit(contextRequest())
            emit(settingsRequest("missing-session-test"))
        }

        val result = runCatching { stubWithoutSession.gigaVoice(requests).toList() }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(StatusException::class.java)
    }

    @Test
    fun `should fail when token header is missing`() = runItTest {
        setupFullModeStubs(efsAdapterMock, gigaVoiceAgentMock)

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
            emit(contextRequest())
            emit(settingsRequest("missing-token-test"))
        }

        val result = runCatching { stubWithoutToken.gigaVoice(requests).toList() }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(StatusException::class.java)
    }

    @Test
    fun `should fail when edu_id header is missing`() = runItTest {
        setupFullModeStubs(efsAdapterMock, gigaVoiceAgentMock)

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
            emit(contextRequest())
            emit(settingsRequest("missing-edu-id-test"))
        }

        val result = runCatching { stubWithoutEduId.gigaVoice(requests).toList() }

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(StatusException::class.java)
    }
}
