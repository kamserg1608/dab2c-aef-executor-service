package ru.sbrf.dab2c.executor.voice.audit

/** Auditor for external service interaction events. */
interface ExternalInteractionAuditor {

    /** Records a successful external interaction. */
    suspend fun success(request: ExternalInteractionRequest)

    /** Records a failed external interaction. */
    suspend fun failed(request: ExternalInteractionRequest)
}

/** Data describing an external interaction for audit purposes. */
data class ExternalInteractionRequest(
    val answerCode: String,
    val rqMessage: String? = null,
    val rsMessage: String? = null,
    val errorCode: String? = null,
    val errorTitle: String? = null
)
