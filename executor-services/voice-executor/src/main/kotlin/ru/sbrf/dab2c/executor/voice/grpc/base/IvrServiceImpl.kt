package ru.sbrf.dab2c.executor.voice.grpc.base

import kotlinx.coroutines.flow.Flow
import net.devh.boot.grpc.server.service.GrpcService
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceServiceGrpcKt
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.logging.MaskingCollector
import ru.sbrf.dab2c.executor.voice.factory.api.ChunkProcessingServiceFactory
import ru.sbrf.dab2c.executor.voice.grpc.client.GigaVoiceClient
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.logging.VoiceMdcInitializer
import ru.sbrf.dab2c.executor.voice.service.api.SessionInitService

/** gRPC service implementation that handles IVR voice requests and orchestrates the processing pipeline. */
@GrpcService
class IvrServiceImpl(
    private val gigaVoiceClient: GigaVoiceClient,
    private val chunkProcessingServiceFactory: ChunkProcessingServiceFactory,
    private val sessionInitService: SessionInitService,
) : GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineImplBase() {
    override fun gigaVoice(requests: Flow<GigaVoiceRequest>): Flow<GigaVoiceResponse> {
        val headers = GrpcMetadataContext.fromGrpcThread()
        val chunkProcessingService = chunkProcessingServiceFactory.create()

        VoiceMdcInitializer.initializeForRequest(headers)
        headers.getHeaderOrNull(RequestHeader.TOKEN)?.let { MaskingCollector.register(it) }

        return with(sessionInitService) {
            chunkProcessingService.processRequestChunks(requests)
                .let { gigaVoiceClient.session(it) }
                .let { chunkProcessingService.processResponseChunks(it) }
                .withSessionContext(headers)
        }
    }
}
