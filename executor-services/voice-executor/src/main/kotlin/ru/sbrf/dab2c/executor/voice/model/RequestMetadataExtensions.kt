package ru.sbrf.dab2c.executor.voice.model

import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.domain.configuration.SessionConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo

/**
 * Converts RequestMetadata to GigaAgentRequestContext for GigaAgent API calls.
 */
fun RequestMetadata.toGigaAgentContext(
    sessionConfiguration: SessionConfiguration,
    conversationId: String,
    daSessionInfo: DaSessionInfo
) = GigaAgentRequestContext(
    ufsSession = this.getHeader(RequestHeader.SESSION),
    ufsToken = this.getHeader(RequestHeader.TOKEN),
    channel = sessionConfiguration.channel,
    conversationId = conversationId,
    eduId = this.getHeader(RequestHeader.EDU_ID),
    daRequestId = this.getHeader(RequestHeader.X_REQUEST_ID),
    daSessionId = daSessionInfo.meta.sessionId,
    daChannel = sessionConfiguration.channel,
    daPlatform = sessionConfiguration.platform,
    daUcpId = daSessionInfo.meta.ucpId
)

/** Builds EFS cookie. */
val RequestMetadata.ufsCookie: String
    get() = "UFS_TOKEN=${this.getHeader(RequestHeader.TOKEN)};UFS_SESSION=${this.getHeader(RequestHeader.SESSION)}"
