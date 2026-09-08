package ru.sbrf.dab2c.executor.clients.giga.agent.audit

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.FUNCTIONS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.POSTPROCESS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.SETTINGS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.clients.giga.agent.model.PostProcessResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsResult
import ru.sbrf.dab2c.executor.clients.giga.agent.util.GigaAgentContextBuilder
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.audit.model.AuditMessageSchema
import ru.sbrf.dab2c.executor.library.audit.model.InteractionAuditRequest
import ru.sbrf.dab2c.executor.library.audit.port.InteractionAuditor
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.library.context.currentSessionInfo

private val logger = KotlinLogging.logger {}

/**
 * Decorator that sends audit events for GigaVoice agent interactions.
 */
class AuditedGigaVoiceAgentClientDecorator(
    private val delegate: GigaVoiceAgentClient,
    private val auditor: InteractionAuditor,
    private val objectMapper: ObjectMapper,
    private val receiver: String
) : GigaVoiceAgentClient {

    override suspend fun getSettings(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        voiceSettings: Settings,
        contextData: DialogContext
    ): SettingsResult {
        val context = GigaAgentContextBuilder.buildRequestContext(
            conversationId, currentSessionInfo(), currentHeaders()
        )
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            auditFailure(
                rqMessage = rqMessage,
                e = e,
                errorCode = ERROR_CODE_SETTINGS
            )
            throw e
        }
    }

    override suspend fun executeFunctionCall(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCalling,
        contextData: DialogContext
    ): FunctionCallResult {
        val context = GigaAgentContextBuilder.buildRequestContext(
            conversationId, currentSessionInfo(), currentHeaders()
        )
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            auditFailure(
                rqMessage = rqMessage,
                e = e,
                errorCode = ERROR_CODE_FUNCTION
            )
            throw e
        }
    }

    override suspend fun postProcess(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        contextData: DialogContext
    ): PostProcessResult {
        val context = GigaAgentContextBuilder.buildRequestContext(
            conversationId, currentSessionInfo(), currentHeaders()
        )
        logger.debug { "GigaVoice postProcess -> receiver=$receiver, conversationId=${context.conversationId}" }

        val rqMessage = buildPostProcessRqMessage(context, agentConfiguration, contextData)

        try {
            val rs = delegate.postProcess(
                conversationId = conversationId,
                agentConfiguration = agentConfiguration,
                contextData = contextData
            )

            auditSuccess(rqMessage, rs)
            return rs
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            auditFailure(
                rqMessage = rqMessage,
                e = e,
                errorCode = ERROR_CODE_POSTPROCESS
            )
            throw e
        }
    }

    private fun buildSettingsRqMessage(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        voiceSettings: Settings,
        contextData: DialogContext
    ): String =
        toJson(
            baseRqMap(
                endpoint = SETTINGS_ENDPOINT,
                context = context,
                agentConfiguration = agentConfiguration,
                contextData = contextData
            ) + mapOf(
                "voiceSettings" to voiceSettings
            )
        )

    private fun buildFunctionRqMessage(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCalling,
        contextData: DialogContext
    ): String =
        toJson(
            baseRqMap(
                endpoint = FUNCTIONS_ENDPOINT,
                context = context,
                agentConfiguration = agentConfiguration,
                contextData = contextData
            ) + mapOf(
                "functionCalling" to functionCalling
            )
        )

    private fun buildPostProcessRqMessage(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        contextData: DialogContext
    ): String =
        toJson(
            baseRqMap(
                endpoint = POSTPROCESS_ENDPOINT,
                context = context,
                agentConfiguration = agentConfiguration,
                contextData = contextData
            )
        )

    private fun baseRqMap(
        endpoint: String,
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        contextData: DialogContext
    ): Map<String, Any?> =
        mapOf(
            "endpoint" to endpoint,
            "receiver" to receiver,
            "conversationId" to context.conversationId,
            "eduId" to context.eduId,
            "ufsSession" to context.ufsSession,
            "channel" to context.channel,
            "agentConfiguration" to agentConfiguration,
            "contextData" to contextData
        )

    private suspend fun auditSuccess(rqMessage: String, response: Any) {
        auditor.success(
            request = InteractionAuditRequest(
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
            request = InteractionAuditRequest(
                answerCode = AuditMessageSchema.ANSWER_CODE_FAIL,
                rqMessage = rqMessage,
                rsMessage = null,
                errorCode = errorCode,
                errorTitle = e.message ?: e::class.simpleName ?: AuditMessageSchema.DEFAULT_ERROR_TITLE
            )
        )
    }

    private fun toJson(value: Any?): String =
        objectMapper.writeValueAsString(value)

    /** Audit error codes for GigaVoice agent interactions. */
    companion object {
        const val ERROR_CODE_SETTINGS: String = "GIGAVOICE_SETTINGS_ERROR"
        const val ERROR_CODE_FUNCTION: String = "GIGAVOICE_FUNCTION_ERROR"
        const val ERROR_CODE_POSTPROCESS: String = "GIGAVOICE_POSTPROCESS_ERROR"
    }
}
