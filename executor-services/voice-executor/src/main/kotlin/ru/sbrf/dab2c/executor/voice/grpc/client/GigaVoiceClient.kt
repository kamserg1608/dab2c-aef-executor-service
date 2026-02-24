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
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MonitoringServiceFactory
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric

/** gRPC client for bidirectional streaming communication with the downstream GigaVoice service. */
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

    /** Opens a bidirectional streaming session with the downstream GigaVoice service. */
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
        val headers = currentHeaders()
        return headers.getHeader(RequestHeader.PLATFORM)
    }

    private suspend fun getChannelHeader(): String {
        val headers = currentHeaders()
        return headers.getHeader(RequestHeader.CHANNEL)
    }

    /** Metric tag constants for monitoring stream chunks. */
    companion object {
        const val STREAM_CHUNK_TYPE = "stream_chunk_type"
        const val PROFANITY_CHECK = "profanity_check"
        const val FUNCTION_NAME = "function_name"
    }
}
