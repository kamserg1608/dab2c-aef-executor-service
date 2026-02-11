package ru.sbrf.dab2c.executor.library.audit.api

/**
 * Auditor for agent interaction events.
 *
 * Emits:
 *  - DAB2C_AGENT_INTERACTION
 *  - DAB2C_AGENT_INTERACTION_FAILED
 */
interface AgentInteractionAuditor {

    /**
     * Sends successful interaction event.
     */
    suspend fun success(
        answerCode: String,
        rqMessage: String?,
        rsMessage: String?
    )

    /**
     * Sends failed interaction event.
     */
    suspend fun failed(
        answerCode: String,
        rqMessage: String?,
        rsMessage: String?,
        errorCode: String?,
        errorTitle: String?
    )
}
