package ru.sbrf.dab2c.executor.it.tests.grpc.proxy

import GigaVoiceProtocol.GigaVoice.AdditionalData
import GigaVoiceProtocol.GigaVoice.AgeType
import GigaVoiceProtocol.GigaVoice.Audio
import GigaVoiceProtocol.GigaVoice.ContentFromModel
import GigaVoiceProtocol.GigaVoice.Emotion
import GigaVoiceProtocol.GigaVoice.Error
import GigaVoiceProtocol.GigaVoice.FunctionCall
import GigaVoiceProtocol.GigaVoice.FunctionCalling
import GigaVoiceProtocol.GigaVoice.GenderType
import GigaVoiceProtocol.GigaVoice.GigaChatModelInfo
import GigaVoiceProtocol.GigaVoice.GigaVoiceResponse
import GigaVoiceProtocol.GigaVoice.InputTranscription
import GigaVoiceProtocol.GigaVoice.OutputTranscription
import GigaVoiceProtocol.GigaVoice.PersonIdentity
import GigaVoiceProtocol.GigaVoice.Usage
import GigaVoiceProtocol.GigaVoice.Warning
import com.google.protobuf.ByteString
import com.google.protobuf.Duration
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.ivr.proto.AudioContent
import ru.sbrf.dab2c.executor.clients.ivr.proto.AudioSettings
import ru.sbrf.dab2c.executor.clients.ivr.proto.ContentForSynthesis
import ru.sbrf.dab2c.executor.clients.ivr.proto.ContentFromClient
import ru.sbrf.dab2c.executor.clients.ivr.proto.FirstSpeaker
import ru.sbrf.dab2c.executor.clients.ivr.proto.FunctionResult
import ru.sbrf.dab2c.executor.clients.ivr.proto.GigaChatSettings
import ru.sbrf.dab2c.executor.clients.ivr.proto.InitialContext
import ru.sbrf.dab2c.executor.clients.ivr.proto.Input
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrRequest
import ru.sbrf.dab2c.executor.clients.ivr.proto.Message
import ru.sbrf.dab2c.executor.clients.ivr.proto.Output
import ru.sbrf.dab2c.executor.clients.ivr.proto.Settings
import ru.sbrf.dab2c.executor.it.support.withSession
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

/**
 * Proxy Mode - Chunk Transfer Tests.
 * Verifies that all message types pass through unchanged in proxy mode.
 */
class ProxyChunkTransferTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should transfer Settings request with full configuration`() = runBlocking {
        val settings = Settings.newBuilder()
            .setVoiceCallId("test-call-123")
            .setGigachat(
                GigaChatSettings.newBuilder()
                    .setModel("GigaChat-Pro")
                    .setTemperature(0.7f)
                    .setTopP(0.9f)
                    .setRepetitionPenalty(1.1f)
                    .setProfanityCheck(true)
                    .build()
            )
            .setAudio(
                AudioSettings.newBuilder()
                    .setInput(
                        Input.newBuilder()
                            .setModel("asr-model")
                            .setAudioEncoding(Input.AudioEncoding.PCM_S16LE)
                            .setSampleRate(16000)
                            .build()
                    )
                    .setOutput(
                        Output.newBuilder()
                            .setVoice("Nec")
                            .setAudioEncoding(Output.AudioEncoding.OPUS)
                            .build()
                    )
                    .build()
            )
            .setContext(
                InitialContext.newBuilder()
                    .addMessages(
                        Message.newBuilder()
                            .setRole("system")
                            .setContent("You are a helpful assistant")
                            .build()
                    )
                    .build()
            )
            .setDisableVad(false)
            .setEnableTranscribeInput(true)
            .addFlags("test-flag")
            .setOutputModalities(Settings.OutputModalities.AUDIO_TEXT)
            .setMode(Settings.Mode.RECOGNIZE_GIGACHAT_SYNTHESIS)
            .setFirstSpeaker(
                FirstSpeaker.newBuilder()
                    .setType("bot")
                    .setLockFirstIn(true)
                    .build()
            )
            .setEnableDenoiser(true)
            .setEnablePrefetch(true)
            .setEnablePersonIdentity(true)
            .setEnableWhisper(false)
            .setEnableEmotion(true)
            .build()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(IvrRequest.newBuilder().setSettings(settings).build())
            val received = mock.awaitRequest { it.hasSettings() }.settings

            assertThat(received.voiceCallId).isEqualTo("test-call-123")
            assertThat(received.gigachat.model).isEqualTo("GigaChat-Pro")
            assertThat(received.gigachat.temperature).isEqualTo(0.7f)
            assertThat(received.gigachat.topP).isEqualTo(0.9f)
            assertThat(received.audio.input.audioEncoding.name).isEqualTo("PCM_S16LE")
            assertThat(received.audio.input.sampleRate).isEqualTo(16000)
            assertThat(received.audio.output.voice).isEqualTo("Nec")
            assertThat(received.context.messagesCount).isEqualTo(1)
            assertThat(received.context.getMessages(0).role).isEqualTo("system")
            assertThat(received.disableVad).isFalse()
            assertThat(received.enableTranscribeInput).isTrue()
            assertThat(received.flagsList).contains("test-flag")
            assertThat(received.outputModalities.name).isEqualTo("AUDIO_TEXT")
            assertThat(received.mode.name).isEqualTo("RECOGNIZE_GIGACHAT_SYNTHESIS")
            assertThat(received.firstSpeaker.type).isEqualTo("bot")
            assertThat(received.enableDenoiser).isTrue()
            assertThat(received.enablePrefetch).isTrue()
            assertThat(received.enablePersonIdentity).isTrue()
            assertThat(received.enableWhisper).isFalse()
            assertThat(received.enableEmotion).isTrue()

            mock.sendResponse(createDefaultResponse())
            session.awaitResponse()
        }
    }

    @Test
    fun `should transfer AudioContent with audio bytes`() = runBlocking {
        val audioBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x10, 0x20, 0x30)
        val audioContent = AudioContent.newBuilder()
            .setAudioChunk(ByteString.copyFrom(audioBytes))
            .build()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(
                IvrRequest.newBuilder()
                    .setInput(ContentFromClient.newBuilder().setAudioContent(audioContent).build())
                    .build()
            )

            val received = mock.awaitRequest { it.hasInput() }.input.audioContent
            assertThat(received.audioChunk.toByteArray()).isEqualTo(audioBytes)

            mock.sendResponse(createDefaultResponse())
            session.awaitResponse()
        }
    }

    @Test
    fun `should transfer AudioContent with speech markers`() = runBlocking {
        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(
                IvrRequest.newBuilder()
                    .setInput(
                        ContentFromClient.newBuilder()
                            .setAudioContent(
                                AudioContent.newBuilder()
                                    .setSpeechStart(true)
                                    .setSpeechEnd(false)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            val receivedStart = mock.awaitRequest { it.hasInput() }.input.audioContent
            assertThat(receivedStart.speechStart).isTrue()
            assertThat(receivedStart.speechEnd).isFalse()
            mock.sendResponse(createDefaultResponse())
            session.awaitResponse()

            session.sendRequest(
                IvrRequest.newBuilder()
                    .setInput(
                        ContentFromClient.newBuilder()
                            .setAudioContent(
                                AudioContent.newBuilder()
                                    .setSpeechStart(false)
                                    .setSpeechEnd(true)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            val receivedEnd = mock.awaitRequest { it.hasInput() }.input.audioContent
            assertThat(receivedEnd.speechStart).isFalse()
            assertThat(receivedEnd.speechEnd).isTrue()
            mock.sendResponse(createDefaultResponse())
            session.awaitResponse()
        }
    }

    @Test
    fun `should transfer ContentForSynthesis with TEXT type`() = runBlocking {
        val synthesisContent = ContentForSynthesis.newBuilder()
            .setText("Hello, how can I help you today?")
            .setContentType(ContentForSynthesis.ContentType.TEXT)
            .setIsFinal(true)
            .build()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(
                IvrRequest.newBuilder()
                    .setInput(ContentFromClient.newBuilder().setContentForSynthesis(synthesisContent).build())
                    .build()
            )

            val received = mock.awaitRequest { it.hasInput() }.input.contentForSynthesis
            assertThat(received.text).isEqualTo("Hello, how can I help you today?")
            assertThat(received.contentType.name).isEqualTo("TEXT")
            assertThat(received.isFinal).isTrue()

            mock.sendResponse(createDefaultResponse())
            session.awaitResponse()
        }
    }

    @Test
    fun `should transfer ContentForSynthesis with SSML type`() = runBlocking {
        val ssmlText = """<speak><prosody rate="slow">Welcome to our service</prosody></speak>"""
        val synthesisContent = ContentForSynthesis.newBuilder()
            .setText(ssmlText)
            .setContentType(ContentForSynthesis.ContentType.SSML)
            .setIsFinal(false)
            .build()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(
                IvrRequest.newBuilder()
                    .setInput(ContentFromClient.newBuilder().setContentForSynthesis(synthesisContent).build())
                    .build()
            )

            val received = mock.awaitRequest { it.hasInput() }.input.contentForSynthesis
            assertThat(received.text).isEqualTo(ssmlText)
            assertThat(received.contentType.name).isEqualTo("SSML")
            assertThat(received.isFinal).isFalse()

            mock.sendResponse(createDefaultResponse())
            session.awaitResponse()
        }
    }

    @Test
    fun `should transfer FunctionResult with content and function name`() = runBlocking {
        val functionResult = FunctionResult.newBuilder()
            .setContent("""{"balance": 1500.50, "currency": "RUB"}""")
            .setFunctionName("get_account_balance")
            .build()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(IvrRequest.newBuilder().setFunctionResult(functionResult).build())

            val received = mock.awaitRequest { it.hasFunctionResult() }.functionResult
            assertThat(received.content).isEqualTo("""{"balance": 1500.50, "currency": "RUB"}""")
            assertThat(received.functionName).isEqualTo("get_account_balance")

            mock.sendResponse(createDefaultResponse())
            session.awaitResponse()
        }
    }

    @Test
    fun `should transfer Audio output with all fields`() = runBlocking {
        val audioBytes = byteArrayOf(0x7F, 0x00, 0x7F, 0x00, 0x50, 0x60)
        val audioResponse = GigaVoiceResponse.newBuilder()
            .setOutput(
                ContentFromModel.newBuilder()
                    .setAudio(
                        Audio.newBuilder()
                            .setAudioChunk(ByteString.copyFrom(audioBytes))
                            .setAudioDuration(Duration.newBuilder().setSeconds(5).setNanos(500000000).build())
                            .setIsFinal(true)
                            .build()
                    )
                    .build()
            )
            .build()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest())
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(audioResponse)

            val response = session.awaitResponse()
            val output = response.output
            assertThat(output.hasAudio()).isTrue()
            assertThat(output.audio.audioChunk.toByteArray()).isEqualTo(audioBytes)
            assertThat(output.audio.audioDuration.seconds).isEqualTo(5)
            assertThat(output.audio.audioDuration.nanos).isEqualTo(500000000)
            assertThat(output.audio.isFinal).isTrue()
        }
    }

    @Test
    fun `should transfer AdditionalData output with all fields`() = runBlocking {
        val additionalDataResponse = GigaVoiceResponse.newBuilder()
            .setOutput(
                ContentFromModel.newBuilder()
                    .setAdditionalData(
                        AdditionalData.newBuilder()
                            .setUsage(
                                Usage.newBuilder()
                                    .setPromptTokens(150)
                                    .setCompletionTokens(75)
                                    .setTotalTokens(225)
                                    .setPrecachedPromptTokens(50)
                                    .build()
                            )
                            .setGigachatModelInfo(
                                GigaChatModelInfo.newBuilder()
                                    .setName("GigaChat-Pro")
                                    .setVersion("1.0.0")
                                    .build()
                            )
                            .setFinishReason("stop")
                            .build()
                    )
                    .build()
            )
            .build()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest())
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(additionalDataResponse)

            val response = session.awaitResponse()
            val output = response.output
            assertThat(output.hasAdditionalData()).isTrue()
            assertThat(output.additionalData.usage.promptTokens).isEqualTo(150)
            assertThat(output.additionalData.usage.completionTokens).isEqualTo(75)
            assertThat(output.additionalData.usage.totalTokens).isEqualTo(225)
            assertThat(output.additionalData.usage.precachedPromptTokens).isEqualTo(50)
            assertThat(output.additionalData.gigachatModelInfo.name).isEqualTo("GigaChat-Pro")
            assertThat(output.additionalData.gigachatModelInfo.version).isEqualTo("1.0.0")
            assertThat(output.additionalData.finishReason).isEqualTo("stop")
        }
    }

    @Test
    fun `should transfer interrupted signal`() = runBlocking {
        val interruptedResponse = GigaVoiceResponse.newBuilder()
            .setOutput(
                ContentFromModel.newBuilder()
                    .setInterrupted(true)
                    .build()
            )
            .build()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest())
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(interruptedResponse)

            val response = session.awaitResponse()
            assertThat(response.output.interrupted).isTrue()
        }
    }

    @Test
    fun `should transfer FunctionCalling with all fields`() = runBlocking {
        val functionCallingResponse = GigaVoiceResponse.newBuilder()
            .setFunctionCall(
                FunctionCalling.newBuilder()
                    .setFunctionCall(
                        FunctionCall.newBuilder()
                            .setName("transfer_money")
                            .setArguments("""{"amount": 1000, "to_account": "40817810099910004312"}""")
                            .build()
                    )
                    .setTimestamp(1704067200000L)
                    .build()
            )
            .build()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest())
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(functionCallingResponse)

            val response = session.awaitResponse()
            val functionCall = response.functionCall
            assertThat(functionCall.functionCall.name).isEqualTo("transfer_money")
            assertThat(functionCall.functionCall.arguments)
                .isEqualTo("""{"amount": 1000, "to_account": "40817810099910004312"}""")
            assertThat(functionCall.timestamp).isEqualTo(1704067200000L)
        }
    }

    @Test
    fun `should transfer InputTranscription with all optional fields`() = runBlocking {
        val inputTranscriptionResponse = GigaVoiceResponse.newBuilder()
            .setInputTranscription(
                InputTranscription.newBuilder()
                    .setText("Hello, I need help with my account")
                    .setTimestamp(1704067200000L)
                    .setUnnormalizedText("hello i need help with my account")
                    .setPersonIdentity(
                        PersonIdentity.newBuilder()
                            .setAge(AgeType.ADULT)
                            .setGender(GenderType.FEMALE)
                            .setAgeScore(0.95f)
                            .setGenderScore(0.88f)
                            .build()
                    )
                    .setPrefetch(true)
                    .setWhisper(false)
                    .setEmotion(
                        Emotion.newBuilder()
                            .setPositive(0.1f)
                            .setNeutral(0.85f)
                            .setNegative(0.05f)
                            .build()
                    )
                    .build()
            )
            .build()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest())
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(inputTranscriptionResponse)

            val response = session.awaitResponse()
            val transcription = response.inputTranscription
            assertThat(transcription.text).isEqualTo("Hello, I need help with my account")
            assertThat(transcription.timestamp).isEqualTo(1704067200000L)
            assertThat(transcription.unnormalizedText).isEqualTo("hello i need help with my account")
            assertThat(transcription.personIdentity.age.name).isEqualTo("ADULT")
            assertThat(transcription.personIdentity.gender.name).isEqualTo("FEMALE")
            assertThat(transcription.personIdentity.ageScore).isEqualTo(0.95f)
            assertThat(transcription.personIdentity.genderScore).isEqualTo(0.88f)
            assertThat(transcription.prefetch).isTrue()
            assertThat(transcription.whisper).isFalse()
            assertThat(transcription.emotion.positive).isEqualTo(0.1f)
            assertThat(transcription.emotion.neutral).isEqualTo(0.85f)
            assertThat(transcription.emotion.negative).isEqualTo(0.05f)
        }
    }

    @Test
    fun `should transfer OutputTranscription with all fields`() = runBlocking {
        val outputTranscriptionResponse = GigaVoiceResponse.newBuilder()
            .setOutputTranscription(
                OutputTranscription.newBuilder()
                    .setText("I can help you check your account balance.")
                    .setFunctionsStateId("state-uuid-12345")
                    .setFinishReason("stop")
                    .setTimestamp(1704067200000L)
                    .build()
            )
            .build()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest())
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(outputTranscriptionResponse)

            val response = session.awaitResponse()
            val transcription = response.outputTranscription
            assertThat(transcription.text).isEqualTo("I can help you check your account balance.")
            assertThat(transcription.functionsStateId).isEqualTo("state-uuid-12345")
            assertThat(transcription.finishReason).isEqualTo("stop")
            assertThat(transcription.timestamp).isEqualTo(1704067200000L)
        }
    }

    @Test
    fun `should transfer Warning response`() = runBlocking {
        val warningResponse = GigaVoiceResponse.newBuilder()
            .setWarning(
                Warning.newBuilder()
                    .setMessage("High latency detected, response may be delayed")
                    .build()
            )
            .build()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest())
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(warningResponse)

            val response = session.awaitResponse()
            assertThat(response.hasWarning()).isTrue()
            assertThat(response.warning.message).isEqualTo("High latency detected, response may be delayed")
        }
    }

    @Test
    fun `should transfer Error response`() = runBlocking {
        val errorResponse = GigaVoiceResponse.newBuilder()
            .setError(
                Error.newBuilder()
                    .setStatus(503)
                    .setMessage("Service temporarily unavailable")
                    .build()
            )
            .build()

        withSession(proxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(createSettingsRequest())
            mock.awaitRequest { it.hasSettings() }
            mock.sendResponse(errorResponse)

            val response = session.awaitResponse()
            assertThat(response.hasError()).isTrue()
            assertThat(response.error.status).isEqualTo(503)
            assertThat(response.error.message).isEqualTo("Service temporarily unavailable")
        }
    }

    private fun createSettingsRequest(): IvrRequest =
        IvrRequest.newBuilder()
            .setSettings(Settings.newBuilder().setVoiceCallId("test").build())
            .build()

    private fun createDefaultResponse(): GigaVoiceResponse =
        GigaVoiceResponse.newBuilder()
            .setOutputTranscription(OutputTranscription.newBuilder().setText("response").build())
            .build()
}
