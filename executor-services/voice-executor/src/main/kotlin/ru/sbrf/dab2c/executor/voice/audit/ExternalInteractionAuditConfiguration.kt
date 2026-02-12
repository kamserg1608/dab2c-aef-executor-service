package ru.sbrf.dab2c.executor.voice.audit

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.library.audit.port.AuditEventSender

/**
 * Spring configuration for external voice interaction audit components.
 *
 * Registers:
 *  - [ExternalInteractionAuditor]
 *
 * Responsible for emitting:
 *  - DAB2C_EXTERNAL_INTERACTION
 *  - DAB2C_EXTERNAL_INTERACTION_FAILED
 */
@Configuration
class ExternalInteractionAuditConfiguration {

    /**
     * Provides [ExternalInteractionAuditor] bean used
     * for auditing external voice interaction events.
     */
    @Bean
    fun externalInteractionAuditor(
        auditEventSender: AuditEventSender
    ): ExternalInteractionAuditor =
        ExternalInteractionAuditorImpl(
            senderValue = "dab2c-aef-executor",
            receiver = "voice-external",
            auditEventSender = auditEventSender
        )
}
