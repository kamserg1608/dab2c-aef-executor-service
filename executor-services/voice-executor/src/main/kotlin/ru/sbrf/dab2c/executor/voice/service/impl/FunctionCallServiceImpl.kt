package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.toGigaAgentContext
import ru.sbrf.dab2c.executor.voice.service.api.FunctionCallService

/**
 * Default implementation of FunctionCallService.
 */
class FunctionCallServiceImpl(
    private val processingState: MutableStateFlow<ProcessingState>,
    private val callBackChannel: Channel<VoiceRequest>,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient
) : FunctionCallService {

    private val logger = KotlinLogging.logger {}

    @Suppress("LongMethod")
    override suspend fun callFunction(functionCalling: FunctionCallingData): FunctionCallingData? {
        val functionName = functionCalling.functionCall.name
        val functionOptions = processingState.value.functionRegistry.functions[functionName]

        if (functionOptions?.isBackendFunction != true) {
            logger.debug { "Function '$functionName' proxied to IVR" }
            return functionCalling
        }

        logger.debug { "Executing backend function '$functionName' via agent" }

        val metadata = GrpcMetadataContext.current()
        val state = processingState.value
        val agentConfiguration = checkNotNull(state.agentConfiguration) {
            "AgentConfiguration must be set before function calls"
        }
        val sessionConfiguration = checkNotNull(state.sessionConfiguration) {
            "SessionConfiguration must be set before function calls"
        }
        val conversationId = checkNotNull(state.conversationId) {
            "ConversationId must be set before function calls"
        }

        val context = metadata.toGigaAgentContext(
            sessionConfiguration = sessionConfiguration,
            conversationId = conversationId
        )

        val result = gigaVoiceAgentClient.executeFunctionCall(
            context = context,
            agentConfiguration = agentConfiguration,
            functionCalling = functionCalling
        )

        callBackChannel.send(VoiceRequest.FunctionResult(result))

        return null
    }
}
