package ru.sbrf.dab2c.executor.voice.logging

import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.logging.MdcContext

/** Initializes MDC logging context for voice executor requests. */
object VoiceMdcInitializer {
    private const val SERVICE_NAME = "dab2c-executor"

    /** Populates MDC context with request header values. */
    fun initializeForRequest(headers: Headers) {
        MdcContext.initialize(
            serviceName = SERVICE_NAME,
            traceId = headers.getHeaderOrNull(RequestHeader.X_REQUEST_ID),
            sessionId = headers.getHeaderOrNull(RequestHeader.SESSION),
            channel = headers.getHeaderOrNull(RequestHeader.CHANNEL),
            platform = headers.getHeaderOrNull(RequestHeader.PLATFORM)
        )
    }

    /** Updates MDC context with session and user identifiers. */
    fun updateWithSessionInfo(sessionId: String, ucpId: String) {
        MdcContext.updateSessionId(sessionId)
        MdcContext.updateUserLogin(ucpId)
    }
}
