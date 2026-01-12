package ru.sbrf.dab2c.executor.voice.grpc.base

import GigaVoiceProtocol.GigaVoice
import GigaVoiceProtocol.GigaVoiceServiceGrpcKt
import kotlinx.coroutines.flow.Flow
import net.devh.boot.grpc.server.service.GrpcService
import ru.sbrf.dab2c.executor.voice.grpc.client.GigaVoiceDownstreamClient
import ru.sbrf.dab2c.executor.voice.factory.api.ChunkProcessingServiceFactory

/**
 * Service implementation for Giga-voice proto.
 */
@GrpcService
class GigaVoiceImplBase(
    private val downstreamClient: GigaVoiceDownstreamClient,
    private val chunkProcessingServiceFactory: ChunkProcessingServiceFactory
) : GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineImplBase() {

    override fun gigaVoice(requests: Flow<GigaVoice.GigaVoiceRequest>): Flow<GigaVoice.GigaVoiceResponse> {

        val processingService = chunkProcessingServiceFactory.create()

        return requests
            .let { processingService.processRequestChunks(it) }
            .let { downstreamClient.forward(it) }
            .let { processingService.processResponseChunks(it) }

    }
}