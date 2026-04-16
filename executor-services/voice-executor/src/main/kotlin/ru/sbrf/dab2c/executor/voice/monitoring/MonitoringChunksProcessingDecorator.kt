package ru.sbrf.dab2c.executor.voice.monitoring

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import ru.sbrf.dab2c.executor.domain.voice.ContentFromModel
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricTags
import ru.sbrf.dab2c.executor.library.monitoring.service.api.TimerSampleMetric
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService

private const val UNKNOWN = "unknown"

/**
 * Decorator that records chunk counting metrics, TTFB timing,
 * and response token tracking for voice processing flows.
 */
class MonitoringChunksProcessingDecorator(
    private val delegate: ChunkProcessingService,
    private val metricFactory: MetricFactory
) : ChunkProcessingService {

    private val logger = KotlinLogging.logger { }

    override fun processRequestChunks(
        requestsChunks: Flow<VoiceRequest>
    ): Flow<VoiceRequest> {
        val monitoredChunks = requestsChunks
            .onEach { request ->
                metricFactory.incrementCounter(
                    ExecutorVoiceMetric.GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL,
                    chunkTags(request.chunkTypeName()) + request.incomingTags()
                )
            }

        return delegate.processRequestChunks(monitoredChunks)
            .onEach { request ->
                metricFactory.incrementCounter(
                    ExecutorVoiceMetric.GRPC_OUTGOING_TO_GIGAVOICE_CHUNKS_TOTAL,
                    chunkTags(request.chunkTypeName()) + request.outgoingToGigaVoiceTags()
                )
            }
    }

    override fun processResponseChunks(
        responsesChunks: Flow<VoiceResponse>
    ): Flow<VoiceResponse> {
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

    private suspend fun trackOutgoingToInitiator(response: VoiceResponse) {
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
        response: VoiceResponse,
        ttfbSample: TimerSampleMetric.TimerSample?,
        clearSample: () -> Unit
    ) {
        if (ttfbSample != null && response is VoiceResponse.InputTranscription) {
            ttfbSample.stop()
            clearSample()
            logger.debug { "First InputTranscription received, TTFB measured." }
        }
    }
}

private fun chunkTags(chunkTypeName: String): Map<String, String> =
    mapOf(MetricTags.STREAM_CHUNK_TYPE to chunkTypeName)

private fun VoiceRequest.chunkTypeName(): String = this::class.simpleName!!

private fun VoiceRequest.incomingTags(): Map<String, String> = mapOf(
    MetricTags.FUNCTION_NAME to if (this is VoiceRequest.FunctionResult) {
        result.functionName ?: ""
    } else {
        ""
    }
)

private fun VoiceRequest.outgoingToGigaVoiceTags(): Map<String, String> = mapOf(
    MetricTags.PROFANITY_CHECK to if (this is VoiceRequest.Settings) {
        settings.gigachat?.profanityCheck?.toString() ?: ""
    } else {
        ""
    }
)

private fun VoiceResponse.chunkTypeName(): String = this::class.simpleName!!

private fun VoiceResponse.incomingFromGigaVoiceTags(): Map<String, String> = mapOf(
    MetricTags.FUNCTION_NAME to if (this is VoiceResponse.FunctionCalling) {
        data.functionCall.name
    } else if (this is VoiceResponse.PlatformFunctionProcessing) {
        data.name
    } else {
        ""
    }
)

private fun VoiceResponse.responseTokenInfo(): Pair<Map<String, String>, Double>? {
    val data = ((this as? VoiceResponse.Output)?.content as? ContentFromModel.AdditionalData)?.data
    val totalTokens = data?.usage?.totalTokens ?: return null
    val tags = mapOf(
        "model" to (data.gigachatModelInfo?.name ?: UNKNOWN),
        "version" to (data.gigachatModelInfo?.version ?: UNKNOWN)
    )
    return tags to totalTokens.toDouble()
}
