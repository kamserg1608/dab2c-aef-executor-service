package ru.sbrf.dab2c.executor.it.tests.grpc.full

import GigaVoiceProtocol.GigaVoice.Audio
import GigaVoiceProtocol.GigaVoice.ContentFromModel
import GigaVoiceProtocol.GigaVoice.GigaVoiceResponse
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.google.protobuf.ByteString
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.ivr.proto.AudioContent
import ru.sbrf.dab2c.executor.clients.ivr.proto.AudioSettings
import ru.sbrf.dab2c.executor.clients.ivr.proto.ContentFromClient
import ru.sbrf.dab2c.executor.clients.ivr.proto.Context
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrRequest
import ru.sbrf.dab2c.executor.clients.ivr.proto.Settings
import ru.sbrf.dab2c.executor.it.support.WireMockResponses
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.withSession
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime
import GigaVoiceProtocol.GigaVoice.FunctionCall as GigaVoiceFunctionCall
import GigaVoiceProtocol.GigaVoice.FunctionCalling as GigaVoiceFunctionCalling
import GigaVoiceProtocol.GigaVoice.OutputTranscription as GigaVoiceOutputTranscription

class FunctionCallBlockingBehaviorTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `backend function call is async - audio flows immediately while function executes`() = runItTest {
        setupSettingsWithFunctions()
        setupSlowFunctionEndpoint(FUNCTION_DELAY_MS)

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createContextRequest())
            session.sendRequest(createSettingsRequest())
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(createInitialResponse())
            session.awaitResponse()

            session.sendRequest(createAudioRequest())
            mock.awaitRequest { it.hasInput() }

            mock.sendResponse(
                createFunctionCallingResponse("get_account_balance", """{"account_id": "123"}""")
            )
            mock.sendResponse(createAudioResponse(1))
            mock.sendResponse(createAudioResponse(2))
            mock.sendResponse(createAudioResponse(3))

            val timeToReceiveAllAudio = measureTime {
                withTimeout(5.seconds) {
                    session.awaitResponse { it.hasOutput() }
                    session.awaitResponse { it.hasOutput() }
                    session.awaitResponse { it.hasOutput() }
                }
            }

            assertThat(timeToReceiveAllAudio)
                .describedAs("Audio should arrive immediately, not blocked by 2s function call")
                .isLessThan(1.seconds)

            wireMock.awaitPostCall("/functions")
        }

        gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/functions")))
    }

    @Test
    fun `backend function result arrives at downstream after async execution completes`() = runItTest {
        setupSettingsWithFunctions()
        setupSlowFunctionEndpoint(FUNCTION_DELAY_MS)

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createContextRequest())
            session.sendRequest(createSettingsRequest())
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(createInitialResponse())
            session.awaitResponse()

            session.sendRequest(createAudioRequest())
            mock.awaitRequest { it.hasInput() }

            mock.sendResponse(
                createFunctionCallingResponse("get_account_balance", """{"account_id": "123"}""")
            )
            mock.sendResponse(createAudioResponse(1))

            session.awaitResponse { it.hasOutput() }

            val functionResultRequest = withTimeout(5.seconds) {
                mock.awaitRequest { it.hasFunctionResult() }
            }

            assertThat(functionResultRequest.functionResult.functionName).isEqualTo("get_account_balance")
            assertThat(functionResultRequest.functionResult.content).contains("balance")
        }

        gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/functions")))
    }

    @Test
    fun `IVR function is proxied to client - no backend call made`() = runItTest {
        setupSettingsWithFunctions()

        withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createContextRequest())
            session.sendRequest(createSettingsRequest())
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(createInitialResponse())
            session.awaitResponse()

            session.sendRequest(createAudioRequest())
            mock.awaitRequest { it.hasInput() }

            val totalTime = measureTime {
                mock.sendResponse(
                    createFunctionCallingResponse("transfer_to_operator", """{"reason": "test"}""")
                )
                mock.sendResponse(createAudioResponse(1))
                mock.sendResponse(createAudioResponse(2))

                session.awaitResponse { it.hasFunctionCall() }
                session.awaitResponse { it.hasOutput() }
                session.awaitResponse { it.hasOutput() }
            }

            assertThat(totalTime).isLessThan(1.seconds)
        }

        gigaVoiceAgentMock.verify(0, postRequestedFor(urlEqualTo("/functions")))
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

    private fun setupSlowFunctionEndpoint(delayMs: Int) {
        gigaVoiceAgentMock.stubFor(
            post(urlEqualTo("/functions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(delayMs)
                        .withBody(
                            WireMockResponses.gigaVoiceFunctionsResponse(
                                "get_account_balance",
                                """{"balance": 1000}"""
                            )
                        )
                )
        )
    }

    private fun createContextRequest(): IvrRequest =
        IvrRequest.newBuilder()
            .setContext(
                Context.newBuilder()
                    .setContent("{}")
                    .build()
            )
            .build()

    private fun createSettingsRequest(): IvrRequest =
        IvrRequest.newBuilder()
            .setSettings(
                Settings.newBuilder()
                    .setVoiceCallId("blocking-test-call")
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
                    .setText("Hello")
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

    private fun createAudioResponse(chunkId: Int): GigaVoiceResponse =
        GigaVoiceResponse.newBuilder()
            .setOutput(
                ContentFromModel.newBuilder()
                    .setAudio(
                        Audio.newBuilder()
                            .setAudioChunk(ByteString.copyFrom(byteArrayOf(chunkId.toByte())))
                            .build()
                    )
                    .build()
            )
            .build()

    companion object {
        private const val FUNCTION_DELAY_MS = 2000
    }
}
