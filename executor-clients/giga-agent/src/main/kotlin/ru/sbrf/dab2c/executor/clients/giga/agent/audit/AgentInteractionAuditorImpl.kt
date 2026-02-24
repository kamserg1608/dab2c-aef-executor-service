package ru.sbrf.dab2c.executor.clients.giga.agent.audit

import ru.sbrf.dab2c.executor.library.audit.model.AuditEvent
import ru.sbrf.dab2c.executor.library.audit.model.AuditEventNames
import ru.sbrf.dab2c.executor.library.audit.model.AuditParams
import ru.sbrf.dab2c.executor.library.audit.port.AuditEventSender

/**
 * Default implementation of [AgentInteractionAuditor].
 *
 * Builds audit payload and sends it via [AuditEventSender].
 */
class AgentInteractionAuditorImpl(
    private val senderValue: String,
    private val receiver: String,
    private val auditEventSender: AuditEventSender,
) : AgentInteractionAuditor {

    override suspend fun success(request: AgentInteractionAuditRequest) {
        auditEventSender.sendEvent(
            event = AuditEvent(
                event = AuditEventNames.DAB2C_AGENT_INTERACTION,
                success = true,
                params = interactionParams(request)
            )
        )
    }

    override suspend fun failed(request: AgentInteractionAuditRequest) {
        auditEventSender.sendEvent(
            event = AuditEvent(
                event = AuditEventNames.DAB2C_AGENT_INTERACTION_FAILED,
                success = false,
                params = interactionParams(request)
            )
        )
    }

    private fun interactionParams(request: AgentInteractionAuditRequest): Map<String, String> =
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
