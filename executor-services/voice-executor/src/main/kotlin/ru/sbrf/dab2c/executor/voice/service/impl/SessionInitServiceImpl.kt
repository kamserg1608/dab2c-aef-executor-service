package ru.sbrf.dab2c.executor.voice.service.impl

import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.TypedSdsClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.impl.readDaSessionInfo
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.model.RequestHeader
import ru.sbrf.dab2c.executor.voice.model.ufsCookie
import ru.sbrf.dab2c.executor.voice.service.api.SessionInitService

/**
 * Default implementation of SessionInitService.
 * Fetches DaSessionInfo and stores it in RequestMetadata.
 */
@Service
class SessionInitServiceImpl(
    private val typedSdsClient: TypedSdsClient
) : SessionInitService {

    override suspend fun initialize() {
        val metadata = GrpcMetadataContext.current()
        metadata._daSessionInfo = typedSdsClient.readDaSessionInfo(
            metadata.getHeader(RequestHeader.CHANNEL),
            metadata.ufsCookie
        )
    }
}
