package ru.sbrf.dab2c.executor.clients.iag.mapper

import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.DaSessionInfoApiMapper
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceAgentConfigMapper
import ru.sbrf.dab2c.executor.clients.giga.agent.model.ACLConfig
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SessionConfig
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.clients.iag.generated.model.IagContent
import ru.sbrf.dab2c.executor.clients.iag.generated.model.IagFunctionCall
import ru.sbrf.dab2c.executor.clients.iag.generated.model.IagFunctionRequest
import ru.sbrf.dab2c.executor.clients.iag.generated.model.IagMessage
import ru.sbrf.dab2c.executor.clients.iag.generated.model.IagMessageContent
import ru.sbrf.dab2c.executor.clients.iag.generated.model.IagXMeta
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Builds an IAG function call request from executor context, agent configuration,
 * function call data and session metadata.
 */
@Component
class IagFunctionRequestBuilder {

    /**
     * Creates [IagFunctionRequest] for calling an IAG function.
     */
    @Suppress("LongParameterList")
    fun build(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCalling,
        daSessionInfo: DaSessionInfo,
        contextData: DialogContext
    ): IagFunctionRequest {
        val agentConfig = GigaVoiceAgentConfigMapper.toApiAgentConfig(agentConfiguration)
        val sessionConfig = SessionConfig(channel = context.channel)

        return IagFunctionRequest(
            message = IagMessage(
                messageId = context.daRequestId,
                conversationId = context.conversationId,
                eduId = context.eduId,
                content = IagContent(
                    message = buildMessage(contextData),
                    functionCall = IagFunctionCall(
                        name = functionCalling.functionCall.name,
                        arguments = parseContextJson(functionCalling.functionCall.arguments)
                    ),
                    timestamp = functionCalling.timestamp
                ),
                xMeta = IagXMeta(
                    userInfo = DaSessionInfoApiMapper.toApiUserInfo(daSessionInfo),
                    sessionInfo = DaSessionInfoApiMapper.toApiSessionInfo(daSessionInfo, context)
                )
            ),
            config = ACLConfig(
                agentConfig = agentConfig,
                sessionConfig = sessionConfig
            )
        )
    }

    private fun buildMessage(contextData: DialogContext): List<IagMessageContent> =
        listOf(
            IagMessageContent(
                type = CONTEXT_MESSAGE_TYPE,
                value = ObjectMappers.MAPPER.writeValueAsString(contextData.data)
            )
        )

    private fun parseContextJson(json: String): Map<String, Any> =
        if (json.isEmpty()) emptyMap() else ObjectMappers.MAPPER.readValue(json)

    private companion object {
        private const val CONTEXT_MESSAGE_TYPE = "context"
    }
}
