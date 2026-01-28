package ru.sbrf.dab2c.executor.voice.logging

import ru.sbrf.dab2c.executor.logging.MdcContext
import ru.sbrf.dab2c.executor.voice.model.RequestHeader
import ru.sbrf.dab2c.executor.voice.model.RequestMetadata

/**
 * Voice-executor specific MDC initializer.
 * Populates MDC with request metadata from gRPC headers.
 */
object VoiceMdcInitializer {
    private const val SERVICE_NAME = "dab2c-executor"

    /**
     * Initializes MDC context from gRPC request metadata.
     * Called at the start of each gRPC session.
     */
    fun initializeForRequest(metadata: RequestMetadata) {
        MdcContext.initialize(
            serviceName = SERVICE_NAME,
            traceId = metadata.getHeaderOrNull(RequestHeader.X_REQUEST_ID),
            sessionId = metadata.getHeaderOrNull(RequestHeader.SESSION),
            channel = metadata.getHeaderOrNull(RequestHeader.CHANNEL),
            platform = metadata.getHeaderOrNull(RequestHeader.PLATFORM)
        )
    }

    /**
     * Updates MDC with session info after it's fetched from efs-adapter.
     */
    fun updateWithSessionInfo(sessionId: String, ucpId: String) {
        MdcContext.updateSessionId(sessionId)
        MdcContext.updateUserLogin(ucpId)
    }
}
