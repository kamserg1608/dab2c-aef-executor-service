package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.RequestMetadata
import ru.sbrf.dab2c.executor.voice.model.toGigaAgentContext
import ru.sbrf.dab2c.executor.voice.service.api.AnalyticsPublisher
import ru.sbrf.dab2c.executor.voice.service.api.FunctionCallService
import kotlin.coroutines.CoroutineContext

/**
 * Default implementation of FunctionCallService.
 * Backend function calls are executed asynchronously to avoid blocking the response flow.
 */
class FunctionCallServiceImpl(
    private val processingState: MutableStateFlow<ProcessingState>,
    private val callBackChannel: Channel<VoiceRequest>,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient,
    private val scope: CoroutineScope,
    private val analyticsPublisher: AnalyticsPublisher
) : FunctionCallService {

    private val logger = KotlinLogging.logger {}

    override suspend fun callFunction(functionCalling: FunctionCallingData): FunctionCallingData? {
        val state = processingState.value
        check(state is ProcessingState.Serving) {
            "Expected Serving state for function calls, but was ${state::class.simpleName}"
        }

        val functionName = functionCalling.functionCall.name
        val functionOptions = state.functionRegistry.functions[functionName]

        if (functionOptions?.isBackendFunction != true) {
            logger.debug { "Function '$functionName' proxied to IVR" }
            return functionCalling
        }

        logger.debug { "Executing backend function '$functionName' via agent (async)" }
        val metadata = GrpcMetadataContext.current()
        val metadataContext = GrpcMetadataContext.asCoroutineContext()
        executeBackendFunctionAsync(state, functionCalling, metadata, metadataContext)
        return null
    }

    private fun executeBackendFunctionAsync(
        state: ProcessingState.Serving,
        functionCalling: FunctionCallingData,
        metadata: RequestMetadata,
        metadataContext: CoroutineContext
    ) {
        val context = metadata.toGigaAgentContext(state.conversationId)

        scope.launch {
            withContext(metadataContext) {
                try {
                    val functionCallResult = gigaVoiceAgentClient.executeFunctionCall(
                        context = context,
                        agentConfiguration = state.agentConfiguration,
                        functionCalling = functionCalling,
                        daSessionInfo = metadata.daSessionInfo,
                        contextData = state.contextData
                    )

                    analyticsPublisher.publishAnalytics(functionCallResult.analytics, context.daRequestId)

                    callBackChannel.send(VoiceRequest.FunctionResult(functionCallResult.result))
                } catch (e: Exception) {
                    logger.error(e) { "Failed to execute backend function '${functionCalling.functionCall.name}'" }
                    throw e
                }
            }
        }
    }
}
