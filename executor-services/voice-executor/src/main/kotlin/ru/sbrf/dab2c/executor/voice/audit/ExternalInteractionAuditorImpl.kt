package ru.sbrf.dab2c.executor.voice.audit

import ru.sbrf.dab2c.executor.domain.audit.AuditEvent
import ru.sbrf.dab2c.executor.library.audit.model.AuditEventNames
import ru.sbrf.dab2c.executor.library.audit.model.AuditParams
import ru.sbrf.dab2c.executor.library.audit.port.AuditEventSender

/**
 * Default implementation of [ExternalInteractionAuditor].
 *
 * Builds audit payload and sends it via [AuditEventSender].
 *
 * Emits:
 *  - DAB2C_EXTERNAL_INTERACTION
 *  - DAB2C_EXTERNAL_INTERACTION_FAILED
 */
class ExternalInteractionAuditorImpl(
    private val senderValue: String,
    private val receiver: String,
    private val auditEventSender: AuditEventSender,
) : ExternalInteractionAuditor {

    override suspend fun success(request: ExternalInteractionRequest, cookie: String) {
        auditEventSender.sendEvent(
            event = AuditEvent(
                event = AuditEventNames.DAB2C_EXTERNAL_INTERACTION,
                success = true,
                params = interactionParams(request)
            ),
            cookie = cookie
        )
    }

    override suspend fun failed(request: ExternalInteractionRequest, cookie: String) {
        auditEventSender.sendEvent(
            event = AuditEvent(
                event = AuditEventNames.DAB2C_EXTERNAL_INTERACTION_FAILED,
                success = false,
                params = interactionParams(request)
            ),
            cookie = cookie
        )
    }

    private fun interactionParams(request: ExternalInteractionRequest): Map<String, String> =
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
