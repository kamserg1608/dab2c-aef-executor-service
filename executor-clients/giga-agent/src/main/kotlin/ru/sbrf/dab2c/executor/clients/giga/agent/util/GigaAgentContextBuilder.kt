package ru.sbrf.dab2c.executor.clients.giga.agent.util

import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import java.util.UUID

/**
 * Builds [GigaAgentRequestContext] purely from its arguments. No coroutine-context reads;
 * callers supply session info and request headers explicitly.
 */
object GigaAgentContextBuilder {

    /** Creates a [GigaAgentRequestContext] from the supplied session info and headers. */
    fun buildRequestContext(
        conversationId: String,
        sessionInfo: DaSessionInfo,
        headers: Headers
    ): GigaAgentRequestContext = GigaAgentRequestContext(
        ufsSession = headers.getHeader(RequestHeader.SESSION),
        ufsToken = headers.getHeader(RequestHeader.TOKEN),
        channel = headers.getHeader(RequestHeader.CHANNEL),
        conversationId = conversationId,
        eduId = headers.getHeader(RequestHeader.EDU_ID),
        traceId = headers.getHeader(RequestHeader.X_TRACE_ID),
        daRequestId = UUID.randomUUID().toString(),
        daSessionId = sessionInfo.meta.sessionId,
        daChannel = headers.getHeader(RequestHeader.CHANNEL),
        daPlatform = headers.getHeader(RequestHeader.PLATFORM),
        daUcpId = sessionInfo.meta.ucpId
    )
}
