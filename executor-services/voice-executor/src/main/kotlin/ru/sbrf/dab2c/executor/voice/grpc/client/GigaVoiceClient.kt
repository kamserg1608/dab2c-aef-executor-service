package ru.sbrf.dab2c.executor.voice.grpc.client

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Component
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceServiceGrpcKt
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MonitoringServiceFactory
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric
import ru.sbrf.dab2c.executor.voice.model.RequestHeader
import ru.sbrf.dab2c.executor.voice.util.extensions.currentRequestMetadata

/**
 * gRPC client for GigaVoice service.
 */
@Component
class GigaVoiceClient(
    private val monitoringServiceFactory: MonitoringServiceFactory
) {

    private val logger = KotlinLogging.logger {}

    @GrpcClient("downstream")
    private lateinit var channel: Channel

    private val stub: GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineStub by lazy {
        GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineStub(channel)
    }

    /** Starts a bidirectional streaming session with GigaVoice service. */
    @Suppress("LongMethod")
    fun session(requests: Flow<GigaVoiceRequest>): Flow<GigaVoiceResponse> {
        logger.debug { "Starting bidirectional session with GigaVoice" }

        val monitoredRequests = requests.onEach { request ->

            val chunkType = request::class.simpleName!!
            val profanityCheck = request.settings?.gigachat?.profanityCheck ?: ""

            monitoringServiceFactory.createCounter(
                ExecutorVoiceMetric.GRPC_OUTGOING_FROM_GIGAVOICE_CHUNKS_TOTAL,
                platform = getPlatformHeader(),
                channel = getChannelHeader(),
                tagsMap = mapOf(
                    STREAM_CHUNK_TYPE to chunkType,
                    PROFANITY_CHECK to profanityCheck.toString()
                )
            ).increment()
        }

        return stub.gigaVoice(monitoredRequests).onEach { response ->
            val chunkType = response::class.simpleName!!
            val functionName = if (chunkType == "FunctionCall") {
                response.functionCall.functionCall.name
            } else {
                ""
            }

            monitoringServiceFactory.createCounter(
                ExecutorVoiceMetric.GRPC_INCOMING_FROM_GIGAVOICE_CHUNKS_TOTAL,
                platform = getPlatformHeader(),
                channel = getChannelHeader(),
                tagsMap = mapOf(
                    STREAM_CHUNK_TYPE to chunkType,
                    FUNCTION_NAME to functionName
                )
            ).increment()
        }
    }

    private suspend fun getPlatformHeader(): String {
        val metadata = currentRequestMetadata()
        return metadata.getHeader(RequestHeader.PLATFORM)
    }

    private suspend fun getChannelHeader(): String {
        val metadata = currentRequestMetadata()
        return metadata.getHeader(RequestHeader.CHANNEL)
    }

    /** Constants for metric tags. */
    companion object {
        const val STREAM_CHUNK_TYPE = "stream_chunk_type"
        const val PROFANITY_CHECK = "profanity_check"
        const val FUNCTION_NAME = "function_name"
    }
}
