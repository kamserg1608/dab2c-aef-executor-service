package ru.sbrf.dab2c.executor.library.audit.model

import ru.sbrf.dab2c.executor.domain.audit.AuditEvent

/**
 * Builder for constructing [AuditEvent] payloads according to DAB2C metamodel.
 */
class AuditEventBuilder(
    private val eventName: String,
    private val success: Boolean
) {

    private val params: MutableMap<String, String> = linkedMapOf()

    /** Sets ANSWER_CODE parameter. */
    fun answerCode(value: String?): AuditEventBuilder = apply {
        putIfNotBlank(AuditParams.ANSWER_CODE, value)
    }

    /** Sets EPK_ID parameter. */
    fun epkId(value: String?): AuditEventBuilder = apply {
        putIfNotBlank(AuditParams.EPK_ID, value)
    }

    /** Sets RECEIVER parameter. */
    fun receiver(value: String?): AuditEventBuilder = apply {
        putIfNotBlank(AuditParams.RECEIVER, value)
    }

    /** Sets SENDER parameter. */
    fun sender(value: String?): AuditEventBuilder = apply {
        putIfNotBlank(AuditParams.SENDER, value)
    }

    /** Sets RQ_MESSAGE parameter. */
    fun rqMessage(value: String?): AuditEventBuilder = apply {
        putIfNotBlank(AuditParams.RQ_MESSAGE, value)
    }

    /** Sets RS_MESSAGE parameter. */
    fun rsMessage(value: String?): AuditEventBuilder = apply {
        putIfNotBlank(AuditParams.RS_MESSAGE, value)
    }

    /** Adds error information (ERROR_CODE / ERROR_TITLE). */
    fun error(code: String?, title: String?): AuditEventBuilder = apply {
        putIfNotBlank(AuditParams.ERROR_CODE, code)
        putIfNotBlank(AuditParams.ERROR_TITLE, title)
    }

    /** Builds final [AuditEvent]. */
    fun build(): AuditEvent =
        AuditEvent(
            event = eventName,
            success = success,
            params = params.toMap()
        )

    private fun putIfNotBlank(key: String, value: String?) {
        if (!value.isNullOrBlank()) {
            params[key] = value
        }
    }
}
