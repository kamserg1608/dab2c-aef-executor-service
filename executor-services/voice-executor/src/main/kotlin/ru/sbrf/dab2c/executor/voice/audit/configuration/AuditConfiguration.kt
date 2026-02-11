package ru.sbrf.dab2c.executor.voice.audit.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.AuditClient
import ru.sbrf.dab2c.executor.library.audit.api.AgentInteractionAuditor
import ru.sbrf.dab2c.executor.library.audit.api.ExternalInteractionAuditor
import ru.sbrf.dab2c.executor.library.audit.config.AuditConfig
import ru.sbrf.dab2c.executor.library.audit.context.AuditContextProvider
import ru.sbrf.dab2c.executor.library.audit.impl.AgentInteractionAuditorImpl
import ru.sbrf.dab2c.executor.library.audit.impl.AuditSender
import ru.sbrf.dab2c.executor.library.audit.impl.ExternalInteractionAuditorImpl
import ru.sbrf.dab2c.executor.voice.audit.context.GrpcAuditContextProvider

/**
 * Spring wiring for audit library.
 *
 * Library itself has no Spring dependencies.
 * This configuration connects:
 *  - AuditClient (EFS adapter)
 *  - AuditContextProvider (gRPC metadata)
 *  - AuditSender
 *  - AgentInteractionAuditor
 *  - ExternalInteractionAuditor
 */
@Configuration
class AuditConfiguration {
    /**
     * Provides audit context based on current gRPC request metadata.
     */
    @Bean
    fun auditContextProvider(): AuditContextProvider =
        GrpcAuditContextProvider()

    /**
     * Creates low-level audit sender responsible for dispatching
     * events to EFS audit endpoint.
     */
    @Bean
    fun auditSender(
        auditClient: AuditClient,
        auditContextProvider: AuditContextProvider
    ): AuditSender =
        AuditSender(
            auditClient = auditClient,
            contextProvider = auditContextProvider,
            enabled = AuditConfig.ENABLED
        )

    /**
     * Creates auditor for agent interaction events
     * (DAB2C_AGENT_INTERACTION).
     */
    @Bean
    fun agentInteractionAuditor(
        auditSender: AuditSender
    ): AgentInteractionAuditor =
        AgentInteractionAuditorImpl(
            senderValue = AuditConfig.SENDER,
            receiver = AuditConfig.DEFAULT_RECEIVER,
            auditSender = auditSender,
            epkIdProvider = { null }
        )

    /**
     * Creates auditor for external system interaction events
     * (DAB2C_EXTERNAL_INTERACTION).
     */
    @Bean
    fun externalInteractionAuditor(
        auditSender: AuditSender
    ): ExternalInteractionAuditor =
        ExternalInteractionAuditorImpl(
            senderValue = AuditConfig.SENDER,
            receiver = AuditConfig.DEFAULT_RECEIVER,
            auditSender = auditSender,
            epkIdProvider = { null }
        )
}
