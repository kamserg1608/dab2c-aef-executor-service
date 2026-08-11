package ru.sbrf.dab2c.executor.voice.audit

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.library.audit.InteractionAuditorImpl
import ru.sbrf.dab2c.executor.library.audit.model.AuditEventNames
import ru.sbrf.dab2c.executor.library.audit.port.AuditEventSender
import ru.sbrf.dab2c.executor.library.audit.port.InteractionAuditor

/**
 * Spring configuration for external voice interaction audit components.
 *
 * Responsible for emitting:
 *  - DAB2C_EXTERNAL_INTERACTION
 *  - DAB2C_EXTERNAL_INTERACTION_FAILED
 */
@Configuration
class ExternalInteractionAuditConfiguration {

    /** Provides [InteractionAuditor] bean for auditing external voice interaction events. */
    @Bean
    fun externalInteractionAuditor(
        auditEventSender: AuditEventSender
    ): InteractionAuditor =
        InteractionAuditorImpl(
            eventNames = AuditEventNames.EXTERNAL_INTERACTION,
            senderValue = "dab2c-aef-executor",
            receiver = "voice-external",
            auditEventSender = auditEventSender
        )
}
