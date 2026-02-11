package ru.sbrf.dab2c.executor.library.audit.impl

import ru.sbrf.dab2c.executor.library.audit.api.ExternalInteractionAuditor
import ru.sbrf.dab2c.executor.library.audit.model.AuditEventBuilder
import ru.sbrf.dab2c.executor.library.audit.model.AuditEventNames

/**
 * Default implementation of [ExternalInteractionAuditor].
 *
 * Responsible for emitting external system interaction events.
 */
class ExternalInteractionAuditorImpl(
    private val senderValue: String,
    private val receiver: String,
    private val auditSender: AuditSender,
    private val epkIdProvider: suspend () -> String?
) : ExternalInteractionAuditor {

    override suspend fun success(answerCode: String, rqMessage: String?, rsMessage: String?) {
        auditSender.send(
            AuditEventBuilder(AuditEventNames.DAB2C_EXTERNAL_INTERACTION, success = true)
                .sender(senderValue)
                .receiver(receiver)
                .answerCode(answerCode)
                .epkId(epkIdProvider())
                .rqMessage(rqMessage)
                .rsMessage(rsMessage)
                .build()
        )
    }

    override suspend fun failed(
        answerCode: String,
        rqMessage: String?,
        rsMessage: String?,
        errorCode: String?,
        errorTitle: String?
    ) {
        auditSender.send(
            AuditEventBuilder(AuditEventNames.DAB2C_EXTERNAL_INTERACTION_FAILED, success = false)
                .sender(senderValue)
                .receiver(receiver)
                .answerCode(answerCode)
                .epkId(epkIdProvider())
                .rqMessage(rqMessage)
                .rsMessage(rsMessage)
                .error(errorCode, errorTitle)
                .build()
        )
    }
}
