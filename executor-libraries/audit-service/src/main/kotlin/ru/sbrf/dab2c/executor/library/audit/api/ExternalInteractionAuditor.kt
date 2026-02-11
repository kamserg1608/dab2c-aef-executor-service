package ru.sbrf.dab2c.executor.library.audit.api

/**
 * Auditor for external system interactions.
 *
 * Emits:
 *  - DAB2C_EXTERNAL_INTERACTION
 *  - DAB2C_EXTERNAL_INTERACTION_FAILED
 */
interface ExternalInteractionAuditor {

    /**
     * Sends successful external interaction event.
     */
    suspend fun success(
        answerCode: String,
        rqMessage: String?,
        rsMessage: String?
    )

    /**
     * Sends failed external interaction event.
     */
    suspend fun failed(
        answerCode: String,
        rqMessage: String?,
        rsMessage: String?,
        errorCode: String?,
        errorTitle: String?
    )
}
