package ru.sbrf.dab2c.executor.voice.audit

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.library.audit.port.AuditEventSender

/**
 * Spring configuration for external voice interaction audit components.
 *
 * Registers:
 *  - [ExternalInteractionAuditor]
 *  - Audited [ChunkProcessingService] decorator
 *
 * Responsible for emitting:
 *  - DAB2C_EXTERNAL_INTERACTION
 *  - DAB2C_EXTERNAL_INTERACTION_FAILED
 */
@Configuration
class ExternalInteractionAuditConfiguration {

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
