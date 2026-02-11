package ru.sbrf.dab2c.executor.voice.audit

/**
 * Auditor for external system interactions.
 *
 * Emits:
 *  - DAB2C_EXTERNAL_INTERACTION
 *  - DAB2C_EXTERNAL_INTERACTION_FAILED
 */
interface ExternalInteractionAuditor {

    /**
     * Emits successful voice interaction audit event.
     */
    suspend fun success(request: ExternalInteractionRequest, cookie: String)

    /**
     * Emits failed voice interaction audit event.
     */
    suspend fun failed(request: ExternalInteractionRequest, cookie: String)
}

/**
 * Request object to avoid long parameter lists (Detekt LongParameterList).
 */
data class ExternalInteractionRequest(
    val answerCode: String,
    val rqMessage: String? = null,
    val rsMessage: String? = null,
    val errorCode: String? = null,
    val errorTitle: String? = null
)
