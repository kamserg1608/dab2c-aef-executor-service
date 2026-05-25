package ru.sbrf.dab2c.executor.voice.audit

import ru.sbrf.dab2c.executor.library.audit.model.AuditMessageSchema
import ru.sbrf.dab2c.executor.library.audit.model.InteractionAuditRequest
import ru.sbrf.dab2c.executor.library.audit.port.InteractionAuditor
import ru.sbrf.dab2c.executor.voice.exception.TolerantExceptionRegistry
import ru.sbrf.dab2c.executor.voice.session.observer.TurnCompleted
import ru.sbrf.dab2c.executor.voice.session.observer.VoiceSessionObserver

/**
 * Emits a success audit event per dialog turn, and a failure audit event when the session
 * terminates with a non-tolerant cause.
 */
class DialogTurnAuditor(
    private val auditor: InteractionAuditor,
) : VoiceSessionObserver {

    override suspend fun onTurnCompleted(event: TurnCompleted) {
        auditor.success(
            request = InteractionAuditRequest(
                answerCode = AuditMessageSchema.ANSWER_CODE_OK,
                rqMessage = event.userReplica?.text.orEmpty(),
                rsMessage = event.assistantReplica?.text.orEmpty(),
            )
        )
    }

    override suspend fun onSessionCompleted(cause: Throwable?) {
        if (cause == null || TolerantExceptionRegistry.isTolerant(cause)) return
        auditor.failed(
            request = InteractionAuditRequest(
                answerCode = AuditMessageSchema.ANSWER_CODE_FAIL,
                errorCode = TolerantExceptionRegistry.extractErrorCode(cause),
                errorTitle = cause.message ?: cause::class.simpleName ?: AuditMessageSchema.DEFAULT_ERROR_TITLE,
            )
        )
    }
}
