package ru.sbrf.dab2c.executor.voice.grpc.base

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.slf4j.MDCContext
import net.devh.boot.grpc.server.service.GrpcService
import ru.sbrf.dab2c.executor.clients.gigavoice.mapper.GigaVoiceDomainMapper
import ru.sbrf.dab2c.executor.clients.ivr.mapper.IvrDomainMapper
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrRequest
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrResponse
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrServiceGrpcKt
import ru.sbrf.dab2c.executor.voice.factory.api.ChunkProcessingServiceFactory
import ru.sbrf.dab2c.executor.voice.grpc.client.GigaVoiceClient
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.grpc.context.MetadataElement
import ru.sbrf.dab2c.executor.voice.logging.VoiceMdcInitializer
import ru.sbrf.dab2c.executor.voice.service.api.SessionInitService

/**
 * gRPC service implementation for IVR protocol.
 * Receives IVR requests, converts to domain model, forwards to GigaVoice.
 */
@GrpcService
class IvrServiceImpl(
    private val gigaVoiceClient: GigaVoiceClient,
    private val chunkProcessingServiceFactory: ChunkProcessingServiceFactory,
    private val sessionInitService: SessionInitService,
) : IvrServiceGrpcKt.IvrServiceCoroutineImplBase() {
    override fun session(requests: Flow<IvrRequest>): Flow<IvrResponse> {
        val metadata = GrpcMetadataContext.fromGrpcThread()
        val chunkProcessingService = chunkProcessingServiceFactory.create()

        VoiceMdcInitializer.initializeForRequest(metadata)

        return requests
            .onStart { sessionInitService.initialize() }
            .map { IvrDomainMapper.toDomainRequest(it) }
            .let { chunkProcessingService.processRequestChunks(it) }
            .map { GigaVoiceDomainMapper.toProtoRequest(it) }
            .let { gigaVoiceClient.session(it) }
            .map { GigaVoiceDomainMapper.toDomainResponse(it) }
            .let { chunkProcessingService.processResponseChunks(it) }
            .map { IvrDomainMapper.toProtoResponse(it) }
            .flowOn(MetadataElement(metadata) + MDCContext())
    }
}
