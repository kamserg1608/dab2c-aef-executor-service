package ru.sbrf.dab2c.executor.clients.gigavoice.stub

import com.google.protobuf.ByteString
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineImplBase
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.additionalData
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audio
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.contentFromModel
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaChatModelInfo
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.outputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.usage

private val logger = KotlinLogging.logger {}

private const val FINISH_REASON_STOP = "stop"
private const val AUDIO_CHUNK_SIZE = 1600
private const val STUB_PROMPT_TOKENS = 10
private const val STUB_COMPLETION_TOKENS = 5
private const val STUB_TOTAL_TOKENS = 15

/**
 * Stub implementation of GigaVoice gRPC service for testing.
 * Returns predefined responses for all request types.
 */
@Component
@Profile("STUB-CLIENTS")
class StubGigaVoiceService : GigaVoiceServiceCoroutineImplBase() {

    override fun gigaVoice(requests: Flow<GigaVoiceRequest>): Flow<GigaVoiceResponse> = flow {
        requests.collect { request ->
            logger.info { "Stub received request: ${request.requestCase}" }
            handleRequest(request)
        }
    }

    private suspend fun FlowCollector<GigaVoiceResponse>.handleRequest(request: GigaVoiceRequest) {
        when (request.requestCase) {
            GigaVoiceRequest.RequestCase.SETTINGS -> handleSettings(request)
            GigaVoiceRequest.RequestCase.INPUT -> handleInput(request)
            GigaVoiceRequest.RequestCase.FUNCTION_RESULT -> handleFunctionResult(request)
            else -> logger.warn { "Stub received unknown request type: ${request.requestCase}" }
        }
    }

    private fun handleSettings(request: GigaVoiceRequest) {
        logger.info { "Stub received settings for voice_call_id: ${request.settings.voiceCallId}" }
    }

    private suspend fun FlowCollector<GigaVoiceResponse>.handleInput(request: GigaVoiceRequest) {
        when {
            request.input.hasAudioContent() && request.input.audioContent.speechEnd -> {
                logger.info { "Stub received speech end, sending mock response" }
                emitStandardResponse("Hello from stub service")
            }
            request.input.hasContentForSynthesis() -> {
                val text = request.input.contentForSynthesis.text
                logger.info { "Stub received text for synthesis: $text" }
                emitStandardResponse("Echo: $text")
            }
        }
    }

    private suspend fun FlowCollector<GigaVoiceResponse>.handleFunctionResult(request: GigaVoiceRequest) {
        logger.info { "Stub received function result: ${request.functionResult.functionName}" }
        emitStandardResponse("Function result received")
    }

    private suspend fun FlowCollector<GigaVoiceResponse>.emitStandardResponse(text: String) {
        emit(transcriptionResponse(text))
        emit(audioResponse())
        emit(additionalDataResponse())
    }

    private fun transcriptionResponse(text: String) = gigaVoiceResponse {
        outputTranscription = outputTranscription {
            this.text = text
            functionsStateId = ""
            finishReason = FINISH_REASON_STOP
            timestamp = System.currentTimeMillis()
        }
    }

    private fun audioResponse() = gigaVoiceResponse {
        output = contentFromModel {
            audio = audio {
                audioChunk = ByteString.copyFrom(ByteArray(AUDIO_CHUNK_SIZE))
                isFinal = true
            }
        }
    }

    private fun additionalDataResponse() = gigaVoiceResponse {
        output = contentFromModel {
            additionalData = additionalData {
                usage = usage {
                    promptTokens = STUB_PROMPT_TOKENS
                    completionTokens = STUB_COMPLETION_TOKENS
                    totalTokens = STUB_TOTAL_TOKENS
                }
                gigachatModelInfo = gigaChatModelInfo {
                    name = "stub-model"
                    version = "1.0"
                }
                finishReason = FINISH_REASON_STOP
            }
        }
    }
}
