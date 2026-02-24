package ru.sbrf.dab2c.executor.clients.giga.agent.audit

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.FUNCTIONS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.SETTINGS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsResult
import ru.sbrf.dab2c.executor.clients.giga.agent.util.GigaAgentContextBuilder
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.library.audit.model.AuditMessageSchema

private val logger = KotlinLogging.logger {}

/**
 * Decorator that sends audit events for GigaVoice agent interactions.
 */
class AuditedGigaVoiceAgentClientDecorator(
    private val delegate: GigaVoiceAgentClient,
    private val auditor: AgentInteractionAuditor,
    private val objectMapper: ObjectMapper,
    private val receiver: String
) : GigaVoiceAgentClient {

    override suspend fun getSettings(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        voiceSettings: VoiceSettings,
        contextData: ContextData
    ): SettingsResult {
        val context = GigaAgentContextBuilder.buildRequestContext(conversationId)
        logger.debug { "GigaVoice getSettings -> receiver=$receiver, conversationId=${context.conversationId}" }

        val rqMessage = buildSettingsRqMessage(context, agentConfiguration, voiceSettings, contextData)

        try {
            val rs = delegate.getSettings(
                conversationId = conversationId,
                agentConfiguration = agentConfiguration,
                voiceSettings = voiceSettings,
                contextData = contextData
            )

            auditSuccess(rqMessage, rs)
            return rs
        } catch (e: Throwable) {
            auditFailure(
                rqMessage = rqMessage,
                e = e,
                errorCode = AuditMessageSchema.ERROR_CODE_GIGAVOICE_SETTINGS
            )
            throw e
        }
    }

    override suspend fun executeFunctionCall(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCallingData,
        contextData: ContextData
    ): FunctionCallResult {
        val context = GigaAgentContextBuilder.buildRequestContext(conversationId)
        logger.debug { "GigaVoice executeFunctionCall -> receiver=$receiver, conversationId=${context.conversationId}" }

        val rqMessage = buildFunctionRqMessage(context, agentConfiguration, functionCalling, contextData)

        try {
            val rs = delegate.executeFunctionCall(
                conversationId = conversationId,
                agentConfiguration = agentConfiguration,
                functionCalling = functionCalling,
                contextData = contextData
            )

            auditSuccess(rqMessage, rs)
            return rs
        } catch (e: Throwable) {
            auditFailure(
                rqMessage = rqMessage,
                e = e,
                errorCode = AuditMessageSchema.ERROR_CODE_GIGAVOICE_FUNCTION
            )
            throw e
        }
    }

    private fun buildSettingsRqMessage(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        voiceSettings: VoiceSettings,
        contextData: ContextData
    ): String =
        toJson(
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
    ): String =
        toJson(
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
    ): Map<String, Any?> =
        mapOf(
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
            request = AgentInteractionAuditRequest(
                answerCode = AuditMessageSchema.ANSWER_CODE_OK,
                rqMessage = rqMessage,
                rsMessage = toJson(response)
            )
        )
    }

    private suspend fun auditFailure(
        rqMessage: String,
        e: Throwable,
        errorCode: String
    ) {
        auditor.failed(
            request = AgentInteractionAuditRequest(
                answerCode = AuditMessageSchema.ANSWER_CODE_FAIL,
                rqMessage = rqMessage,
                rsMessage = null,
                errorCode = errorCode,
                errorTitle = e.message ?: AuditMessageSchema.DEFAULT_ERROR_TITLE
            )
        )
    }

    private fun toJson(value: Any?): String =
        objectMapper.writeValueAsString(value)
}
