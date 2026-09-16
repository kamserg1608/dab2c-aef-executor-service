package ru.sbrf.dab2c.executor.voice.monitoring

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.ContentFromClient
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.ContentFromModel
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricTags
import ru.sbrf.dab2c.executor.library.monitoring.service.api.TimerSampleMetric
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService

private const val UNKNOWN = "unknown"
private const val UNKNOWN_CHUNK_TYPE = "Unknown"

/**
 * Decorator that records chunk counting metrics, TTFB timing,
 * response token tracking, and settings initialization duration
 * for voice processing flows.
 */
class MonitoringChunksProcessingDecorator(
    private val delegate: ChunkProcessingService,
    private val metricFactory: MetricFactory
) : ChunkProcessingService {
    private var settingsInitSample: TimerSampleMetric.TimerSample? = null
    private val logger = KotlinLogging.logger { }

    override fun processRequestChunks(
        requestsChunks: Flow<GigaVoiceRequest>
    ): Flow<GigaVoiceRequest> {

        val monitoredChunks = requestsChunks
            .onEach { request ->
                trackSettingsStart(request)
                metricFactory.incrementCounter(
                    ExecutorVoiceMetric.GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL,
                    chunkTags(request.chunkTypeName()) + request.incomingTags()
                )
            }

        return delegate.processRequestChunks(monitoredChunks)
            .onEach { request ->
                trackSettingsEnd(request)
                metricFactory.incrementCounter(
                    ExecutorVoiceMetric.GRPC_OUTGOING_TO_GIGAVOICE_CHUNKS_TOTAL,
                    chunkTags(request.chunkTypeName()) + request.outgoingToGigaVoiceTags()
                )
            }
    }

    private suspend fun trackSettingsStart(request: GigaVoiceRequest) {
        if (isSettingsRequest(request)) {
            settingsInitSample = metricFactory
                .createTimerSample(ExecutorVoiceMetric.GRPC_SETTINGS_INITIALIZATION_DURATION_SECONDS)
                .start()
        }
    }

    private fun trackSettingsEnd(request: GigaVoiceRequest) {
        if (isSettingsRequest(request) && settingsInitSample != null) {
            settingsInitSample?.stop()
            settingsInitSample = null
        }
    }

    private fun isSettingsRequest(request: GigaVoiceRequest) =
        request.requestCase == GigaVoiceRequest.RequestCase.SETTINGS

    override fun processResponseChunks(
        responsesChunks: Flow<GigaVoiceResponse>
    ): Flow<GigaVoiceResponse> {
        var ttfbSample: TimerSampleMetric.TimerSample? = null

        val monitoredChunks = responsesChunks
            .onStart {
                ttfbSample = metricFactory.createTimerSample(
                    ExecutorVoiceMetric.GRPC_CONNECTIONS_TTFB_SECONDS
                ).start()
            }
            .onEach { response ->
                metricFactory.incrementCounter(
                    ExecutorVoiceMetric.GRPC_INCOMING_FROM_GIGAVOICE_CHUNKS_TOTAL,
                    chunkTags(response.chunkTypeName()) + response.incomingFromGigaVoiceTags()
                )
            }

        return delegate.processResponseChunks(monitoredChunks)
            .onEach { response ->
                trackOutgoingToInitiator(response)
                measureTtfb(response, ttfbSample) { ttfbSample = null }
            }
    }

    private suspend fun trackOutgoingToInitiator(response: GigaVoiceResponse) {
        metricFactory.incrementCounter(
            ExecutorVoiceMetric.GRPC_OUTGOING_TO_INITIATOR_CHUNKS_TOTAL,
            chunkTags(response.chunkTypeName())
        )

        response.responseTokenInfo()?.let { (tokenTags, tokenCount) ->
            metricFactory.incrementCounter(
                ExecutorVoiceMetric.GRPC_RESPONSE_TOTAL_TOKENS,
                tokenCount,
                chunkTags(response.chunkTypeName()) + tokenTags
            )
        }
    }

    private fun measureTtfb(
        response: GigaVoiceResponse,
        ttfbSample: TimerSampleMetric.TimerSample?,
        clearSample: () -> Unit
    ) {
        if (ttfbSample != null && response.responseCase == GigaVoiceResponse.ResponseCase.INPUT_TRANSCRIPTION) {
            ttfbSample.stop()
            clearSample()
            logger.debug { "First InputTranscription received, TTFB measured." }
        }
    }
}

private fun chunkTags(chunkTypeName: String): Map<String, String> =
    mapOf(MetricTags.STREAM_CHUNK_TYPE to chunkTypeName)

private fun GigaVoiceRequest.chunkTypeName(): String = when (requestCase) {
    GigaVoiceRequest.RequestCase.SETTINGS -> "Settings"
    GigaVoiceRequest.RequestCase.INPUT -> when (input.contentCase) {
        ContentFromClient.ContentCase.AUDIO_CONTENT -> "Audio"
        ContentFromClient.ContentCase.CONTENT_FOR_SYNTHESIS -> "TextForSynthesis"
        ContentFromClient.ContentCase.CONTENT_NOT_SET, null -> "Input"
    }
    GigaVoiceRequest.RequestCase.FUNCTION_RESULT -> "FunctionResult"
    GigaVoiceRequest.RequestCase.CONTEXT -> "Context"
    GigaVoiceRequest.RequestCase.REQUEST_NOT_SET, null -> UNKNOWN_CHUNK_TYPE
}

private fun GigaVoiceRequest.incomingTags(): Map<String, String> = mapOf(
    MetricTags.FUNCTION_NAME to if (requestCase == GigaVoiceRequest.RequestCase.FUNCTION_RESULT) {
        functionResult.functionName
    } else {
        ""
    }
)

private fun GigaVoiceRequest.outgoingToGigaVoiceTags(): Map<String, String> = mapOf(
    MetricTags.PROFANITY_CHECK to if (requestCase == GigaVoiceRequest.RequestCase.SETTINGS && settings.hasGigachat()) {
        settings.gigachat.profanityCheck.toString()
    } else {
        ""
    }
)

private fun GigaVoiceResponse.chunkTypeName(): String = when (responseCase) {
    GigaVoiceResponse.ResponseCase.OUTPUT -> "Output"
    GigaVoiceResponse.ResponseCase.FUNCTION_CALL -> "FunctionCalling"
    GigaVoiceResponse.ResponseCase.INPUT_TRANSCRIPTION -> "InputTranscription"
    GigaVoiceResponse.ResponseCase.OUTPUT_TRANSCRIPTION -> "OutputTranscription"
    GigaVoiceResponse.ResponseCase.ERROR -> "Error"
    GigaVoiceResponse.ResponseCase.WARNING -> "Warning"
    GigaVoiceResponse.ResponseCase.INPUT_FILES -> "InputFiles"
    GigaVoiceResponse.ResponseCase.PLATFORM_FUNCTION_PROCESSING -> "PlatformFunctionProcessing"
    GigaVoiceResponse.ResponseCase.SERVICE_INFO -> "ServiceInfo"
    GigaVoiceResponse.ResponseCase.RESPONSE_NOT_SET, null -> UNKNOWN_CHUNK_TYPE
}

private fun GigaVoiceResponse.incomingFromGigaVoiceTags(): Map<String, String> = mapOf(
    MetricTags.FUNCTION_NAME to when (responseCase) {
        GigaVoiceResponse.ResponseCase.FUNCTION_CALL -> functionCall.functionCall.name
        GigaVoiceResponse.ResponseCase.PLATFORM_FUNCTION_PROCESSING -> platformFunctionProcessing.name
        else -> ""
    }
)

private fun GigaVoiceResponse.responseTokenInfo(): Pair<Map<String, String>, Double>? {
    val additional = takeIf { it.responseCase == GigaVoiceResponse.ResponseCase.OUTPUT }
        ?.output
        ?.takeIf { it.responseCase == ContentFromModel.ResponseCase.ADDITIONAL_DATA }
        ?.additionalData
        ?.takeIf { it.hasUsage() }
        ?: return null
    val modelInfo = additional.takeIf { it.hasGigachatModelInfo() }?.gigachatModelInfo
    val tags = mapOf(
        "model" to (modelInfo?.name ?: UNKNOWN),
        "version" to (modelInfo?.version ?: UNKNOWN)
    )
    return tags to additional.usage.totalTokens.toDouble()
}
