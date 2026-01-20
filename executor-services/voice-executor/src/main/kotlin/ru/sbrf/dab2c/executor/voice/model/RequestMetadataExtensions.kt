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
    ufsSession = session,
    ufsToken = token,
    channel = sessionConfiguration.channel,
    conversationId = conversationId,
    eduId = eduId,
    daRequestId = requestId,
    daSessionId = daSessionInfo.meta.sessionId,
    daChannel = sessionConfiguration.channel,
    daPlatform = sessionConfiguration.platform,
    daUcpId = daSessionInfo.meta.ucpId
)
