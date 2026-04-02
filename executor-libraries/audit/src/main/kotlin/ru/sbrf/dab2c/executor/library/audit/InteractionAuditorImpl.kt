package ru.sbrf.dab2c.executor.library.audit

import ru.sbrf.dab2c.executor.library.audit.model.AuditEvent
import ru.sbrf.dab2c.executor.library.audit.model.AuditEventNamePair
import ru.sbrf.dab2c.executor.library.audit.model.AuditParams
import ru.sbrf.dab2c.executor.library.audit.model.InteractionAuditRequest
import ru.sbrf.dab2c.executor.library.audit.port.AuditEventSender
import ru.sbrf.dab2c.executor.library.audit.port.InteractionAuditor

/** Implementation of [InteractionAuditor] that sends audit events via [AuditEventSender]. */
class InteractionAuditorImpl(
    private val eventNames: AuditEventNamePair,
    private val senderValue: String,
    private val receiver: String,
    private val auditEventSender: AuditEventSender,
) : InteractionAuditor {

    override suspend fun success(request: InteractionAuditRequest) {
        auditEventSender.sendEvent(
            event = AuditEvent(
                event = eventNames.success,
                success = true,
                params = interactionParams(request)
            )
        )
    }

    override suspend fun failed(request: InteractionAuditRequest) {
        auditEventSender.sendEvent(
            event = AuditEvent(
                event = eventNames.failed,
                success = false,
                params = interactionParams(request)
            )
        )
    }

    private fun interactionParams(request: InteractionAuditRequest): Map<String, String> =
        buildMap {
            putIfNotBlank(AuditParams.SENDER, senderValue)
            putIfNotBlank(AuditParams.RECEIVER, receiver)
            putIfNotBlank(AuditParams.ANSWER_CODE, request.answerCode)
            putIfNotBlank(AuditParams.RQ_MESSAGE, request.rqMessage)
            putIfNotBlank(AuditParams.RS_MESSAGE, request.rsMessage)
            putIfNotBlank(AuditParams.ERROR_CODE, request.errorCode)
            putIfNotBlank(AuditParams.ERROR_TITLE, request.errorTitle)
        }

    private fun MutableMap<String, String>.putIfNotBlank(key: String, value: String?) {
        if (!value.isNullOrBlank()) put(key, value)
    }
}
