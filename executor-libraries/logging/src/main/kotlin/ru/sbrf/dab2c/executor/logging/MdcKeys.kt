package ru.sbrf.dab2c.executor.logging

/**
 * MDC (Mapped Diagnostic Context) key constants for structured logging.
 */
object MdcKeys {
    // Common fields (set at request start, propagated everywhere)
    const val TYPE = "type"
    const val TRACE_ID = "traceId"
    const val RQ_UID = "requestId"
    const val SESSION_ID = "sessionId"
    const val CHANNEL = "channel"
    const val PLATFORM = "platform"
    const val USER_LOGIN = "userLogin"
    const val SERVICE_NAME = "serviceName"
    const val TENANT_CODE = "tenantCode"
    const val DEPLOYMENT_UNIT = "deploymentUnit"
    const val BLOCK = "block"

    // Integration-specific fields
    const val RQ_MESSAGE = "rqMessage"
    const val RS_MESSAGE = "rsMessage"
    const val EXECUTION_TIME_LONG = "executionTimeLong"
    const val DESTINATION_SERVICE = "destinationService"
    const val DESTINATION_SYSTEM = "destinationSystem"
    const val STATUS_CODE = "statusCode"
    const val ERROR_CODE = "errorCode"
    const val CLASS_NAME = "className"
    const val SERVER_EVENT_DATETIME = "serverEventDatetime"
    const val SPAN_ID = "spanId"
    const val PARENT_SPAN_ID = "parentSpanId"
}
