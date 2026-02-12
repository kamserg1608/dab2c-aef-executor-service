package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ProfileClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.TypedSdsClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.impl.readDaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
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
    private val configuratorClient: ConfiguratorClient,
    private val profileClient: ProfileClient
) : SessionInitService {

    private val logger = KotlinLogging.logger {}

    @Suppress("LongMethod")
    override suspend fun initialize() {
        val metadata = currentRequestMetadata()
        val channel = metadata.getHeader(RequestHeader.CHANNEL)
        logger.info { "Session initialization started for channel=$channel" }

        val daSessionMeta = typedSdsClient.readDaSessionMeta(
            channel,
            metadata.ufsCookie
        )
        logger.debug { "Fetched DaSessionMeta: sessionId=${daSessionMeta.sessionId}, ucpId=${daSessionMeta.ucpId}" }

        val daSessionCommon = configuratorClient.getDaSessionCommon(metadata.ufsCookie)

        logger.debug { "Fetched DaSessionCommon. $daSessionCommon" }

        val daSessionUserInfo = profileClient.getPersonInfo(metadata.ufsCookie)

        logger.debug { "Fetched DaSessionUserInfo. $daSessionUserInfo" }

        metadata._daSessionInfo = DaSessionInfo(daSessionMeta, daSessionCommon, daSessionUserInfo)

        val daSessionInfo = metadata.daSessionInfo
        VoiceMdcInitializer.updateWithSessionInfo(
            sessionId = daSessionInfo.meta.sessionId,
            ucpId = daSessionInfo.meta.ucpId
        )
        logger.info { "Session initialization completed, MDC updated: sessionId=${daSessionInfo.meta.sessionId}" }
    }
}
