package ru.sbrf.dab2c.executor.clients.giga.agent.audit

import ru.sbrf.dab2c.executor.clients.efs.adapter.api.AuditClient
import ru.sbrf.dab2c.executor.domain.audit.AuditEvent
import ru.sbrf.dab2c.executor.library.audit.model.AuditEventNames
import ru.sbrf.dab2c.executor.library.audit.model.AuditParams

/**
 * Default implementation of [AgentInteractionAuditor].
 *
 * Builds audit payload and sends it via [AuditClient].
 */
class AgentInteractionAuditorImpl(
    private val senderValue: String,
    private val receiver: String,
    private val auditClient: AuditClient,
) : AgentInteractionAuditor {

    override suspend fun success(request: AgentInteractionAuditRequest, cookie: String) {
        auditClient.sendEvent(
            AuditEvent(
                event = AuditEventNames.DAB2C_AGENT_INTERACTION,
                success = true,
                params = interactionParams(request)
            ),
            cookie
        )
    }

    override suspend fun failed(request: AgentInteractionAuditRequest, cookie: String) {
        auditClient.sendEvent(
            AuditEvent(
                event = AuditEventNames.DAB2C_AGENT_INTERACTION_FAILED,
                success = false,
                params = interactionParams(request)
            ),
            cookie
        )
    }

    private fun interactionParams(request: AgentInteractionAuditRequest): Map<String, String> =
        buildMap<String, String> {
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
