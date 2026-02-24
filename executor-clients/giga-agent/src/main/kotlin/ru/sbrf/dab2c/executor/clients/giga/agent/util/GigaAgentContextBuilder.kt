package ru.sbrf.dab2c.executor.clients.giga.agent.util

import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.library.context.currentSessionInfo

/**
 * Builds [GigaAgentRequestContext] from the current coroutine context headers and session info.
 */
object GigaAgentContextBuilder {

    /** Creates a [GigaAgentRequestContext] populated from the current coroutine context. */
    suspend fun buildRequestContext(conversationId: String): GigaAgentRequestContext {
        val headers = currentHeaders()
        val sessionInfo = currentSessionInfo()
        return GigaAgentRequestContext(
            ufsSession = headers.getHeader(RequestHeader.SESSION),
            ufsToken = headers.getHeader(RequestHeader.TOKEN),
            channel = headers.getHeader(RequestHeader.CHANNEL),
            conversationId = conversationId,
            eduId = headers.getHeader(RequestHeader.EDU_ID),
            daRequestId = headers.getHeader(RequestHeader.X_REQUEST_ID),
            daSessionId = sessionInfo.meta.sessionId,
            daChannel = headers.getHeader(RequestHeader.CHANNEL),
            daPlatform = headers.getHeader(RequestHeader.PLATFORM),
            daUcpId = sessionInfo.meta.ucpId
        )
    }
}
