package ru.sbrf.dab2c.executor.logging

import org.slf4j.MDC

private const val DEFAULT_TYPE = "SYSTEM"
private const val DEFAULT_TENANT_CODE = "DAB2C"

/**
 * Utility for initializing and updating MDC (Mapped Diagnostic Context).
 * MDC provides thread-local storage for log context fields.
 */
object MdcContext {

    /**
     * Initializes MDC with request context.
     * Called at the start of each gRPC session.
     */
    @Suppress("LongParameterList")
    fun initialize(
        serviceName: String,
        traceId: String? = null,
        requestId: String? = null,
        sessionId: String? = null,
        channel: String? = null,
        platform: String? = null,
        userLogin: String? = null
    ) {
        // gRPC executor threads are reused between sessions. Without clearing MDC,
        // optional fields and MaskingCollector values (including tokens) from the
        // previous session remain in the ThreadLocal and grow on every request.
        MDC.clear()

        MDC.put(MdcKeys.TYPE, DEFAULT_TYPE)
        MDC.put(MdcKeys.SERVICE_NAME, serviceName)
        MDC.put(MdcKeys.TENANT_CODE, DEFAULT_TENANT_CODE)
        traceId?.let { MDC.put(MdcKeys.TRACE_ID, it) }
        requestId?.let { MDC.put(MdcKeys.RQ_UID, it) }
        sessionId?.let { MDC.put(MdcKeys.SESSION_ID, it) }
        channel?.let { MDC.put(MdcKeys.CHANNEL, it) }
        platform?.let { MDC.put(MdcKeys.PLATFORM, it) }
        userLogin?.let { MDC.put(MdcKeys.USER_LOGIN, it) }
        AdditionalMdcConfig.keys.forEach { (key, value) -> MDC.put(key, value) }
    }

    /**
     * Updates user login in MDC after session info is fetched.
     */
    fun updateUserLogin(userLogin: String) {
        MDC.put(MdcKeys.USER_LOGIN, userLogin)
    }

    /**
     * Updates session ID in MDC after session info is fetched.
     */
    fun updateSessionId(sessionId: String) {
        MDC.put(MdcKeys.SESSION_ID, sessionId)
    }
}
