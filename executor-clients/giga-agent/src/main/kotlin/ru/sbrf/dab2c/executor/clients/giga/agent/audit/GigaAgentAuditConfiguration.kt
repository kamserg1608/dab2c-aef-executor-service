package ru.sbrf.dab2c.executor.clients.giga.agent.audit

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.AuditClient

/**
 * Spring configuration for Giga Agent audit components.
 *
 * Registers:
 *  - [AgentInteractionAuditor]
 *
 * Responsible for emitting:
 *  - DAB2C_AGENT_INTERACTION
 *  - DAB2C_AGENT_INTERACTION_FAILED
 */
@Configuration
class GigaAgentAuditConfiguration {

    /**
     * Auditor bean for agent interactions.
     */
    @Bean
    fun agentInteractionAuditor(
        auditClient: AuditClient
    ): AgentInteractionAuditor =
        AgentInteractionAuditorImpl(
            senderValue = "dab2c-aef-executor",
            receiver = "giga-voice-agent",
            auditClient = auditClient
        )
}
