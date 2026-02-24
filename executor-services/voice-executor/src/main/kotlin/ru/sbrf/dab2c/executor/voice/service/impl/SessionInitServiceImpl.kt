package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ProfileClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.TypedSdsClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.impl.readDaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.voice.logging.VoiceMdcInitializer
import ru.sbrf.dab2c.executor.voice.service.api.SessionInitService

/** Initializes voice session by loading session metadata from SDS and EFS. */
@Service
class SessionInitServiceImpl(
    private val typedSdsClient: TypedSdsClient,
    private val configuratorClient: ConfiguratorClient,
    private val profileClient: ProfileClient
) : SessionInitService {

    private val logger = KotlinLogging.logger {}

    @Suppress("LongMethod")
    override suspend fun initialize(): DaSessionInfo {
        val headers = currentHeaders()
        val channel = headers.getHeader(RequestHeader.CHANNEL)
        logger.info { "Session initialization started for channel=$channel" }

        val daSessionMeta = typedSdsClient.readDaSessionMeta(channel)
        logger.debug { "Fetched DaSessionMeta: sessionId=${daSessionMeta.sessionId}, ucpId=${daSessionMeta.ucpId}" }

        val daSessionCommon = configuratorClient.getDaSessionCommon()
        logger.debug { "Fetched DaSessionCommon. $daSessionCommon" }

        val daSessionUserInfo = profileClient.getPersonInfo()
        logger.debug { "Fetched DaSessionUserInfo. $daSessionUserInfo" }

        val daSessionInfo = DaSessionInfo(daSessionMeta, daSessionCommon, daSessionUserInfo)

        VoiceMdcInitializer.updateWithSessionInfo(
            sessionId = daSessionInfo.meta.sessionId,
            ucpId = daSessionInfo.meta.ucpId
        )
        logger.info { "Session initialization completed, MDC updated: sessionId=${daSessionInfo.meta.sessionId}" }

        return daSessionInfo
    }
}
