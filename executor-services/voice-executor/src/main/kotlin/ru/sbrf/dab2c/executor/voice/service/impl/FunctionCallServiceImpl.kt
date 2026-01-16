package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.voice.config.properties.VoiceExecutorConfigurationProperties
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.FunctionCallService

/**
 * Default implementation of FunctionCallService.
 */
class FunctionCallServiceImpl(
    private val processingState: MutableStateFlow<ProcessingState>,
    private val callBackChannel: Channel<VoiceRequest>,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient,
    private val configProperties: VoiceExecutorConfigurationProperties
) : FunctionCallService {

    private val logger = KotlinLogging.logger {}

    override suspend fun callFunction(functionCalling: FunctionCallingData): FunctionCallingData? {
        val functionName = functionCalling.functionCall.name
        val functionOptions = processingState.value.functionRegistry.functions[functionName]

        if (functionOptions?.isBackendFunction != true) {
            logger.debug { "Function '$functionName' proxied to IVR" }
            return functionCalling
        }

        logger.debug { "Executing backend function '$functionName' via agent" }

        val metadata = GrpcMetadataContext.current()
        val agentConfiguration = checkNotNull(processingState.value.agentConfiguration) {
            "AgentConfiguration must be set before function calls"
        }

        val result = gigaVoiceAgentClient.executeFunctionCall(
            ufsSession = metadata.session,
            ufsToken = metadata.token,
            functionCalling = functionCalling,
            agentConfiguration = agentConfiguration,
            channel = configProperties.channel,
            conversationId = metadata.conversationId ?: metadata.session,
            eduId = metadata.eduId ?: metadata.session
        )

        callBackChannel.send(VoiceRequest.FunctionResult(result))

        return null
    }
}
