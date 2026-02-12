package ru.sbrf.dab2c.executor.clients.efs.adapter.configuration.audit

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.AuditClient
import ru.sbrf.dab2c.executor.library.audit.port.AuditEventSender

/**
 * Spring configuration that exposes [AuditEventSender] for the application context.
 *
 * This bean is used by audit libraries/components and is implemented via EFS [AuditClient].
 */
@Configuration
class EfsAuditSenderConfiguration {

    /**
     * Provides [AuditEventSender] implementation backed by [AuditClient].
     */
    @Bean
    fun auditEventSender(auditClient: AuditClient): AuditEventSender =
        EfsAuditEventSender(auditClient)
}
