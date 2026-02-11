package ru.sbrf.dab2c.executor.voice.model

import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext

/**
 * Converts RequestMetadata to GigaAgentRequestContext for GigaAgent API calls.
 */
fun RequestMetadata.toGigaAgentContext(
    conversationId: String
) = GigaAgentRequestContext(
    ufsSession = this.getHeader(RequestHeader.SESSION),
    ufsToken = this.getHeader(RequestHeader.TOKEN),
    channel = this.getHeader(RequestHeader.CHANNEL),
    conversationId = conversationId,
    eduId = this.getHeader(RequestHeader.EDU_ID),
    daRequestId = this.getHeader(RequestHeader.X_REQUEST_ID),
    daSessionId = this.daSessionInfo.meta.sessionId,
    daChannel = this.getHeader(RequestHeader.CHANNEL),
    daPlatform = this.getHeader(RequestHeader.PLATFORM),
    daUcpId = this.daSessionInfo.meta.ucpId
)

/** Builds EFS cookie. */
val RequestMetadata.ufsCookie: String
    get() = "UFS-TOKEN=${this.getHeader(RequestHeader.TOKEN)};UFS-SESSION=${this.getHeader(RequestHeader.SESSION)}"
