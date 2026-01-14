package ru.sbrf.dab2c.executor.voice.grpc.base

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.devh.boot.grpc.server.service.GrpcService
import ru.sbrf.dab2c.executor.clients.gigavoice.mapper.GigaVoiceDomainMapper
import ru.sbrf.dab2c.executor.clients.ivr.mapper.IvrDomainMapper
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrRequest
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrResponse
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrServiceGrpcKt
import ru.sbrf.dab2c.executor.voice.factory.api.ChunkProcessingServiceFactory
import ru.sbrf.dab2c.executor.voice.grpc.client.GigaVoiceClient
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext

/**
 * gRPC service implementation for IVR protocol.
 * Receives IVR requests, converts to domain model, forwards to GigaVoice.
 */
@GrpcService
class IvrServiceImpl(
    private val gigaVoiceClient: GigaVoiceClient,
    private val chunkProcessingServiceFactory: ChunkProcessingServiceFactory,
) : IvrServiceGrpcKt.IvrServiceCoroutineImplBase() {

    private val logger = KotlinLogging.logger {}

    override fun session(requests: Flow<IvrRequest>): Flow<IvrResponse> {
        val metadata = GrpcMetadataContext.current()
        logger.debug { "Starting session with ${metadata.size} metadata entries" }

        val chunkProcessingService = chunkProcessingServiceFactory.create(metadata)
        return requests
            .map { IvrDomainMapper.toDomainRequest(it) }
            .let { chunkProcessingService.processRequestChunks(it) }
            .map { GigaVoiceDomainMapper.toProtoRequest(it) }
            .let { gigaVoiceClient.session(it) }
            .map { GigaVoiceDomainMapper.toDomainResponse(it) }
            .let { chunkProcessingService.processResponseChunks(it) }
            .map { IvrDomainMapper.toProtoResponse(it) }
    }
}
