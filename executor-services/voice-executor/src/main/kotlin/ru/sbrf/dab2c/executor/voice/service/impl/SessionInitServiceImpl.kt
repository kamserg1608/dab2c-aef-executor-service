package ru.sbrf.dab2c.executor.voice.service.impl

import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.TypedSdsClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.impl.readDaSessionInfo
import ru.sbrf.dab2c.executor.voice.logging.VoiceMdcInitializer
import ru.sbrf.dab2c.executor.voice.model.RequestHeader
import ru.sbrf.dab2c.executor.voice.model.ufsCookie
import ru.sbrf.dab2c.executor.voice.service.api.SessionInitService
import ru.sbrf.dab2c.executor.voice.util.extensions.currentRequestMetadata

/**
 * Default implementation of SessionInitService.
 * Fetches DaSessionInfo and stores it in RequestMetadata.
 */
@Service
class SessionInitServiceImpl(
    private val typedSdsClient: TypedSdsClient
) : SessionInitService {

    override suspend fun initialize() {
        val metadata = currentRequestMetadata()
        metadata._daSessionInfo = typedSdsClient.readDaSessionInfo(
            metadata.getHeader(RequestHeader.CHANNEL),
            metadata.ufsCookie
        )

        val daSessionInfo = metadata.daSessionInfo
        VoiceMdcInitializer.updateWithSessionInfo(
            sessionId = daSessionInfo.meta.sessionId,
            ucpId = daSessionInfo.meta.ucpId
        )
    }
}
