package ru.sbrf.dab2c.executor.it.tests.grpc.full

import GigaVoiceProtocol.GigaVoice.GigaVoiceResponse
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.ivr.proto.AudioContent
import ru.sbrf.dab2c.executor.clients.ivr.proto.AudioSettings
import ru.sbrf.dab2c.executor.clients.ivr.proto.ContentFromClient
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrRequest
import ru.sbrf.dab2c.executor.clients.ivr.proto.Settings
import ru.sbrf.dab2c.executor.it.support.WireMockResponses
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.withSession
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import GigaVoiceProtocol.GigaVoice.FunctionCall as GigaVoiceFunctionCall
import GigaVoiceProtocol.GigaVoice.FunctionCalling as GigaVoiceFunctionCalling
import GigaVoiceProtocol.GigaVoice.OutputTranscription as GigaVoiceOutputTranscription

/**
 * Full Mode - Function Call Routing Tests.
 * Verifies routing of FunctionCalling between IVR and backend execution.
 */
class FullFunctionCallRoutingTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should proxy IVR function to client when isBackendFunction is false`() = runItTest {
        setupSettingsWithFunctions()

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest("ivr-function-test"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(createInitialResponse())
            session.awaitResponse()

            session.sendRequest(createAudioRequest())
            mock.awaitRequest { it.hasInput() }

            mock.sendResponse(
                createFunctionCallingResponse("transfer_to_operator", """{"reason": "customer request"}""")
            )

            val functionResponse = session.awaitResponse { it.hasFunctionCall() }
            assertThat(functionResponse.functionCall.functionCall.name)
                .isEqualTo("transfer_to_operator")
        }

        gigaVoiceAgentMock.verify(0, postRequestedFor(urlEqualTo("/functions")))
    }

    @Test
    fun `should proxy unknown function to IVR client by default`() = runItTest {
        setupSettingsWithFunctions()

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest("unknown-function-test"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(createInitialResponse())
            session.awaitResponse()

            session.sendRequest(createAudioRequest())
            mock.awaitRequest { it.hasInput() }

            mock.sendResponse(createFunctionCallingResponse("unknown_function_not_in_registry", """{"data": "test"}"""))

            val functionResponse = session.awaitResponse { it.hasFunctionCall() }
            assertThat(functionResponse.functionCall.functionCall.name)
                .isEqualTo("unknown_function_not_in_registry")
        }

        gigaVoiceAgentMock.verify(0, postRequestedFor(urlEqualTo("/functions")))
    }

    @Test
    fun `should execute backend function via GigaAgent when isBackendFunction is true`() = runItTest {
        setupSettingsWithFunctions()
        setupFunctionsEndpoint("get_account_balance", """{"balance": 1000}""")

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest("backend-function-test"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(createInitialResponse())
            session.awaitResponse()

            session.sendRequest(createAudioRequest())
            mock.awaitRequest { it.hasInput() }

            mock.sendResponse(createFunctionCallingResponse("get_account_balance", """{"account_id": "12345"}"""))

            wireMock.awaitPostCall("/functions")

            assertThat(session.receivedResponses.none { it.hasFunctionCall() }).isTrue()
        }

        gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/functions")))
    }

    @Test
    fun `should fail with error when GigaAgent functions endpoint returns 500`() = runItTest {
        setupSettingsWithFunctions()

        gigaVoiceAgentMock.stubFor(
            post(urlEqualTo("/functions"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")
                )
        )

        val result = runCatching {
            withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(createSettingsRequest("functions-error-test"))
                mock.awaitRequest { it.hasSettings() }

                mock.sendResponse(createInitialResponse())
                session.awaitResponse()

                session.sendRequest(createAudioRequest())
                mock.awaitRequest { it.hasInput() }

                mock.sendResponse(createFunctionCallingResponse("get_account_balance", """{"account_id": "12345"}"""))

                wireMock.awaitPostCall("/functions")

                session.awaitResponse()
            }
        }

        assertThat(result.isFailure).isTrue()
        gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/functions")))
    }

    private fun setupSettingsWithFunctions() {
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
                        .withBody(WireMockResponses.GIGA_VOICE_SETTINGS_WITH_FUNCTIONS_RESPONSE)
                )
        )
    }

    private fun setupFunctionsEndpoint(functionName: String, resultContent: String) {
        gigaVoiceAgentMock.stubFor(
            post(urlEqualTo("/functions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.gigaVoiceFunctionsResponse(functionName, resultContent))
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

    private fun createAudioRequest(): IvrRequest =
        IvrRequest.newBuilder()
            .setInput(
                ContentFromClient.newBuilder()
                    .setAudioContent(
                        AudioContent.newBuilder()
                            .setSpeechStart(true)
                            .build()
                    )
                    .build()
            )
            .build()

    private fun createInitialResponse(): GigaVoiceResponse =
        GigaVoiceResponse.newBuilder()
            .setOutputTranscription(
                GigaVoiceOutputTranscription.newBuilder()
                    .setText("Hello, how can I help you?")
                    .build()
            )
            .build()

    private fun createFunctionCallingResponse(name: String, arguments: String): GigaVoiceResponse =
        GigaVoiceResponse.newBuilder()
            .setFunctionCall(
                GigaVoiceFunctionCalling.newBuilder()
                    .setFunctionCall(
                        GigaVoiceFunctionCall.newBuilder()
                            .setName(name)
                            .setArguments(arguments)
                            .build()
                    )
                    .setTimestamp(System.currentTimeMillis())
                    .build()
            )
            .build()
}
