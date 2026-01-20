package ru.sbrf.dab2c.executor.voice.model

import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.domain.configuration.SessionConfiguration

// TODO: Replace with actual values from session data in next iteration
private const val STUB_DA_SESSION_ID = "STUB-da-session-id"
private const val STUB_DA_UCP_ID = "STUB-da-ucp-id"

/**
 * Converts RequestMetadata to GigaAgentRequestContext for GigaAgent API calls.
 */
fun RequestMetadata.toGigaAgentContext(
    sessionConfiguration: SessionConfiguration,
    conversationId: String
) = GigaAgentRequestContext(
    ufsSession = session,
    ufsToken = token,
    channel = sessionConfiguration.channel,
    conversationId = conversationId,
    eduId = eduId,
    daRequestId = requestId,
    daSessionId = STUB_DA_SESSION_ID,
    daChannel = sessionConfiguration.channel,
    daPlatform = sessionConfiguration.platform,
    daUcpId = STUB_DA_UCP_ID
)
