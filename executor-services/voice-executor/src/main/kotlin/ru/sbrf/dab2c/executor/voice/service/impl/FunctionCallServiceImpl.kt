package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.voice.config.properties.VoiceExecutorConfigurationProperties
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.FunctionCallService
import ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.api.GigaVoiceAgentClient

/**
 * Default implementation of FunctionCallService.
 */
class FunctionCallServiceImpl(
    private val processingState: MutableStateFlow<ProcessingState>,
    private val callBackChannel: Channel<VoiceRequest>,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient,
    private val configuratorClient: ConfiguratorClient,
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
        val session = checkNotNull(metadata.session) { "Session header is required" }
        val token = checkNotNull(metadata.token) { "Token header is required" }
        val ufsCookie = checkNotNull(metadata.ufsCookie) { "UFS cookie could not be constructed" }

        val agentConfiguration = configuratorClient.getRestAgentConfig(
            agentName = configProperties.agentName,
            cookie = ufsCookie
        )

        val result = gigaVoiceAgentClient.executeFunctionCall(
            ufsSession = session,
            ufsToken = token,
            functionCalling = functionCalling,
            agentConfiguration = agentConfiguration,
            channel = configProperties.channel
        )

        callBackChannel.send(VoiceRequest.FunctionResult(result))

        return null
    }
}
