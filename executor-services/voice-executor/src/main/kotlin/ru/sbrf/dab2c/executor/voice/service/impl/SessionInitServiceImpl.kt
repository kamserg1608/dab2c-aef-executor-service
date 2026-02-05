package ru.sbrf.dab2c.executor.voice.service.impl

import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.TypedSdsClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.impl.readDaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
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
    private val typedSdsClient: TypedSdsClient,
    private val configuratorClient: ConfiguratorClient
) : SessionInitService {

    @Suppress("LongMethod")
    override suspend fun initialize() {
        val metadata = currentRequestMetadata()
        val daSessionMeta = typedSdsClient.readDaSessionMeta(
            metadata.getHeader(RequestHeader.CHANNEL),
            metadata.ufsCookie
        )

        val daSessionCommon = configuratorClient.getDaSessionCommon(metadata.ufsCookie)

        val daSessionUserInfo = DaSessionUserInfo(
            firstName = "",
            patrName = "",
            birthDay = "",
            segmentCodeType = "",
            ucpId = ""
        )

        metadata._daSessionInfo = DaSessionInfo(daSessionMeta, daSessionCommon, daSessionUserInfo)

        val daSessionInfo = metadata.daSessionInfo
        VoiceMdcInitializer.updateWithSessionInfo(
            sessionId = daSessionInfo.meta.sessionId,
            ucpId = daSessionInfo.meta.ucpId
        )
    }
}
