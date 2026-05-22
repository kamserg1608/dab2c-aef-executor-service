package ru.sbrf.dab2c.executor.voice.service.impl

import com.google.protobuf.ByteString
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.additionalData
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audio
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audioContent
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audioSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.contentFromClient
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.contentFromModel
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.inputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.outputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.usage
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.session.observer.Replica
import ru.sbrf.dab2c.executor.voice.session.observer.VoiceSessionObserver
import ru.sbrf.dab2c.executor.voice.session.observer.VoiceSettings
import ru.sbrf.dab2c.executor.voice.test.IncrementingTimeProvider
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCall as protoFunctionCallDsl
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.settings as protoSettings

/**
 * Coverage for the primitive observer events: session start/complete, settings, user/assistant
 * replica boundaries, function call/result ordering, usage, warnings, errors.
 */
class VoiceSessionObserverPrimitiveEmissionsTest {

    private lateinit var delegate: ChunkProcessingService
    private lateinit var observer: VoiceSessionObserver
    private lateinit var accumulator: VoiceSessionObserverDelegate

    @BeforeEach
    fun setUp() {
        delegate = mockk()
        observer = mockk(relaxed = true)
        every { delegate.processRequestChunks(any()) } answers { firstArg() }
        every { delegate.processResponseChunks(any()) } answers { firstArg() }
        accumulator = VoiceSessionObserverDelegate(delegate, observer, IncrementingTimeProvider())
    }

    @Test
    fun `onSessionStarted fires exactly once on session bootstrap`() = runTest {
        accumulator.processRequestChunks(
            flowOf(gigaVoiceRequest { settings = protoSettings { voiceCallId = "c1" } })
        ).toList()
        accumulator.processResponseChunks(
            flowOf(gigaVoiceResponse { inputTranscription = inputTranscription { text = "hi" } })
        ).toList()

        coVerify(exactly = 1) { observer.onSessionStarted() }
    }

    @Test
    fun `onSettingsReceived fires with mapped VoiceSettings`() = runTest {
        accumulator.processRequestChunks(
            flowOf(
                gigaVoiceRequest {
                    settings = protoSettings {
                        voiceCallId = "call-42"
                        audio = audioSettings { }
                    }
                }
            )
        ).toList()

        val slot = slot<VoiceSettings>()
        coVerify(exactly = 1) { observer.onSettingsReceived(capture(slot)) }
        assertThat(slot.captured.voiceCallId).isEqualTo("call-42")
    }

    @Test
    fun `onUserReplicaStarted fires on first audio chunk only`() = runTest {
        accumulator.processRequestChunks(
            flowOf(
                audioRequest(),
                audioRequest(),
                audioRequest(),
            )
        ).toList()

        coVerify(exactly = 1) { observer.onUserReplicaStarted(any()) }
    }

    @Test
    fun `onUserReplicaCompleted fires when assistant transcription starts`() = runTest {
        val responses = flowOf(
            inputTranscriptionResponse("Hello"),
            inputTranscriptionResponse(" world"),
            outputTranscriptionResponse("Hi"),
        )

        accumulator.processResponseChunks(responses).toList()

        val slot = slot<Replica>()
        coVerify(exactly = 1) { observer.onUserReplicaCompleted(capture(slot)) }
        assertThat(slot.captured.text).isEqualTo("Hello world")
    }

    @Test
    fun `assistant segment closes on AUDIO is_final and emits onAssistantReplicaCompleted`() = runTest {
        val responses = flowOf(
            outputTranscriptionResponse("Hi"),
            audioResponse(isFinal = false),
            audioResponse(isFinal = true),
        )

        accumulator.processResponseChunks(responses).toList()

        coVerify(exactly = 1) { observer.onAssistantReplicaStarted(any()) }
        coVerify(exactly = 1) { observer.onAssistantReplicaCompleted(any()) }
    }

    @Test
    fun `interrupted emits onAssistantInterrupted plus onAssistantReplicaCompleted`() = runTest {
        val responses = flowOf(
            outputTranscriptionResponse("Hi"),
            gigaVoiceResponse {
                output = contentFromModel { interrupted = true }
            },
        )

        accumulator.processResponseChunks(responses).toList()

        coVerifyOrder {
            observer.onAssistantInterrupted(any())
            observer.onAssistantReplicaCompleted(any())
        }
    }

    @Test
    fun `FUNCTION_CALL closes current segment and order is callReceived before replicaCompleted`() = runTest {
        val responses = flowOf(
            inputTranscriptionResponse("balance?"),
            outputTranscriptionResponse("Let me check"),
            functionCallResponse("get_balance", """{"id":"1"}"""),
            outputTranscriptionResponse("Your balance is 1000"),
            audioResponse(isFinal = true),
            inputTranscriptionResponse("Thanks"),
        )

        accumulator.processResponseChunks(responses).toList()

        coVerifyOrder {
            observer.onAssistantReplicaStarted(any())
            observer.onFunctionCallReceived(any())
            observer.onAssistantReplicaCompleted(any())
            observer.onAssistantReplicaStarted(any())
            observer.onAssistantReplicaCompleted(any())
        }
    }

    @Test
    fun `one turn with function call dance produces two segments and one TurnCompleted`() = runTest {
        val responses = flowOf(
            inputTranscriptionResponse("Q"),
            outputTranscriptionResponse("seg1"),
            functionCallResponse("f", "{}"),
            outputTranscriptionResponse("seg2"),
            audioResponse(isFinal = true),
            inputTranscriptionResponse("Q2"),
        )

        accumulator.processResponseChunks(responses).toList()

        coVerify(exactly = 2) { observer.onAssistantReplicaStarted(any()) }
        coVerify(exactly = 2) { observer.onAssistantReplicaCompleted(any()) }
        // Two complete turns: the closed Q→seg1 seg2 turn, plus the trailing Q2 turn flushed on completion.
        coVerify(exactly = 2) { observer.onTurnCompleted(any()) }
    }

    @Test
    fun `onUsageUpdated fires per usage chunk with accumulated totalTokens`() = runTest {
        val responses = flowOf(
            outputTranscriptionResponse("hi"),
            usageResponse(totalTokens = 10),
            usageResponse(totalTokens = 5),
        )

        accumulator.processResponseChunks(responses).toList()

        coVerify { observer.onUsageUpdated(10) }
        coVerify { observer.onUsageUpdated(15) }
    }

    @Test
    fun `onWarningEmitted and onErrorEmitted fire with mapped payloads`() = runTest {
        val responses = flowOf(
            inputTranscriptionResponse("hi"),
            gigaVoiceResponse {
                warning = ru.sbrf.dab2c.executor.clients.gigavoice.proto.warning { message = "low" }
            },
            gigaVoiceResponse {
                error = ru.sbrf.dab2c.executor.clients.gigavoice.proto.error {
                    status = 500
                    message = "boom"
                }
            },
        )

        accumulator.processResponseChunks(responses).toList()

        coVerify(exactly = 1) {
            observer.onWarningEmitted(match { it.message == "low" })
        }
        coVerify(exactly = 1) {
            observer.onErrorEmitted(match { it.status == 500 && it.message == "boom" })
        }
    }

    @Test
    fun `onFunctionResultSent fires on FUNCTION_RESULT request chunk`() = runTest {
        accumulator.processRequestChunks(
            flowOf(
                gigaVoiceRequest {
                    functionResult = ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionResult {
                        functionName = "f"
                        content = """{"ok":true}"""
                    }
                }
            )
        ).toList()

        coVerify(exactly = 1) {
            observer.onFunctionResultSent(match { it.name == "f" && it.content == """{"ok":true}""" })
        }
    }

    private fun audioRequest(): GigaVoiceRequest = gigaVoiceRequest {
        input = contentFromClient {
            audioContent = audioContent { audioChunk = ByteString.copyFrom(byteArrayOf(1)) }
        }
    }

    private fun inputTranscriptionResponse(text: String): GigaVoiceResponse = gigaVoiceResponse {
        inputTranscription = inputTranscription { this.text = text }
    }

    private fun outputTranscriptionResponse(text: String): GigaVoiceResponse = gigaVoiceResponse {
        outputTranscription = outputTranscription { this.text = text }
    }

    private fun audioResponse(isFinal: Boolean): GigaVoiceResponse = gigaVoiceResponse {
        output = contentFromModel {
            audio = audio {
                audioChunk = ByteString.copyFrom(byteArrayOf(1))
                this.isFinal = isFinal
            }
        }
    }

    private fun usageResponse(totalTokens: Int): GigaVoiceResponse = gigaVoiceResponse {
        output = contentFromModel {
            additionalData = additionalData { usage = usage { this.totalTokens = totalTokens } }
        }
    }

    private fun functionCallResponse(name: String, arguments: String): GigaVoiceResponse = gigaVoiceResponse {
        functionCall = functionCalling {
            functionCall = protoFunctionCallDsl {
                this.name = name
                this.arguments = arguments
            }
        }
    }
}
