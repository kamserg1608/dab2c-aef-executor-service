package ru.sbrf.dab2c.executor.clients.giga.agent.audit

/**
 * Auditor for agent interaction events.
 *
 * Emits:
 *  - DAB2C_AGENT_INTERACTION
 *  - DAB2C_AGENT_INTERACTION_FAILED
 */

interface AgentInteractionAuditor {

    /**
     * Emits successful agent interaction audit event.
     */
    suspend fun success(request: AgentInteractionAuditRequest, cookie: String)

    /**
     * Emits failed agent interaction audit event.
     */
    suspend fun failed(request: AgentInteractionAuditRequest, cookie: String)
}

/**
 * Request object to avoid long parameter lists (Detekt LongParameterList).
 */
data class AgentInteractionAuditRequest(
    val answerCode: String,
    val rqMessage: String? = null,
    val rsMessage: String? = null,
    val errorCode: String? = null,
    val errorTitle: String? = null
)
