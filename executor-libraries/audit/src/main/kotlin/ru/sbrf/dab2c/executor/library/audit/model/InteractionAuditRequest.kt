package ru.sbrf.dab2c.executor.library.audit.model

/** Data describing an interaction for audit purposes. */
data class InteractionAuditRequest(
    val answerCode: String,
    val rqMessage: String? = null,
    val rsMessage: String? = null,
    val errorCode: String? = null,
    val errorTitle: String? = null
)
