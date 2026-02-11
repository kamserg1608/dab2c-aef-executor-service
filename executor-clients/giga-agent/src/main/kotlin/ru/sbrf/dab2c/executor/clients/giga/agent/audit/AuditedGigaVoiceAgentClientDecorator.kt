package ru.sbrf.dab2c.executor.clients.giga.agent.audit

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.FUNCTIONS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.SETTINGS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsResult
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.library.audit.api.AgentInteractionAuditor
import ru.sbrf.dab2c.executor.library.audit.model.AuditMessageSchema

private val logger = KotlinLogging.logger {}

/**
 * Audit decorator for [GigaVoiceAgentClient].
 *
 * Emits audit events:
 *  - DAB2C_AGENT_INTERACTION (success)
 *  - DAB2C_AGENT_INTERACTION_FAILED (failed)
 */
class AuditedGigaVoiceAgentClientDecorator(
    private val delegate: GigaVoiceAgentClient,
    private val auditor: AgentInteractionAuditor,
    private val objectMapper: ObjectMapper,
    private val receiver: String
) : GigaVoiceAgentClient {

    override suspend fun getSettings(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        voiceSettings: VoiceSettings,
        daSessionInfo: DaSessionInfo,
        contextData: ContextData
    ): SettingsResult {
        logger.debug { "GigaVoice getSettings -> receiver=$receiver, conversationId=${context.conversationId}" }

        val rqMessage = buildSettingsRqMessage(context, agentConfiguration, voiceSettings, contextData)

        return runCatching {
            delegate.getSettings(context, agentConfiguration, voiceSettings, daSessionInfo, contextData)
        }.fold(
            onSuccess = { rs -> rs.also { auditSuccess(rqMessage, rs) } },
            onFailure = { e -> throw auditFailure(rqMessage, e, AuditMessageSchema.ERROR_CODE_GIGAVOICE_SETTINGS) }
        )
    }

    override suspend fun executeFunctionCall(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCallingData,
        daSessionInfo: DaSessionInfo,
        contextData: ContextData
    ): FunctionCallResult {
        logger.debug { "GigaVoice executeFunctionCall -> receiver=$receiver, conversationId=${context.conversationId}" }

        val rqMessage = buildFunctionRqMessage(context, agentConfiguration, functionCalling, contextData)

        return runCatching {
            delegate.executeFunctionCall(context, agentConfiguration, functionCalling, daSessionInfo, contextData)
        }.fold(
            onSuccess = { rs -> rs.also { auditSuccess(rqMessage, rs) } },
            onFailure = { e -> throw auditFailure(rqMessage, e, AuditMessageSchema.ERROR_CODE_GIGAVOICE_FUNCTION) }
        )
    }

    private fun buildSettingsRqMessage(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        voiceSettings: VoiceSettings,
        contextData: ContextData
    ): String = safeJson(
        baseRqMap(
            endpoint = SETTINGS_ENDPOINT,
            context = context,
            agentConfiguration = agentConfiguration,
            contextData = contextData
        ) + mapOf(
            AuditMessageSchema.KEY_VOICE_SETTINGS to voiceSettings
        )
    )

    private fun buildFunctionRqMessage(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCallingData,
        contextData: ContextData
    ): String = safeJson(
        baseRqMap(
            endpoint = FUNCTIONS_ENDPOINT,
            context = context,
            agentConfiguration = agentConfiguration,
            contextData = contextData
        ) + mapOf(
            AuditMessageSchema.KEY_FUNCTION_CALLING to functionCalling
        )
    )

    private fun baseRqMap(
        endpoint: String,
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        contextData: ContextData
    ): Map<String, Any?> = mapOf(
        AuditMessageSchema.KEY_ENDPOINT to endpoint,
        AuditMessageSchema.KEY_RECEIVER to receiver,
        AuditMessageSchema.KEY_CONVERSATION_ID to context.conversationId,
        AuditMessageSchema.KEY_EDU_ID to context.eduId,
        AuditMessageSchema.KEY_UFS_SESSION to context.ufsSession,
        AuditMessageSchema.KEY_CHANNEL to context.channel,
        AuditMessageSchema.KEY_AGENT_CONFIGURATION to agentConfiguration,
        AuditMessageSchema.KEY_CONTEXT_DATA to contextData
    )

    private suspend fun auditSuccess(rqMessage: String, response: Any) {
        auditor.success(
            AuditMessageSchema.ANSWER_CODE_OK,
            rqMessage,
            safeJson(response)
        )
    }

    private suspend fun auditFailure(rqMessage: String, e: Throwable, errorCode: String): Throwable {
        logger.warn(e) { "GigaVoice call failed: ${e.message}" }

        auditor.failed(
            AuditMessageSchema.ANSWER_CODE_FAIL,
            rqMessage,
            null,
            errorCode,
            e.message ?: AuditMessageSchema.DEFAULT_ERROR_TITLE
        )
        return e
    }

    private fun safeJson(value: Any?): String =
        runCatching { objectMapper.writeValueAsString(value) }
            .getOrElse { "${value?.javaClass?.simpleName}(serializationError=${it.message})" }
}
