package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ClosedSendChannelException
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.iag.api.IagFunctionClient
import ru.sbrf.dab2c.executor.domain.configuration.FunctionConfig
import ru.sbrf.dab2c.executor.library.common.runCatchingCancellable
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.VoiceSession
import ru.sbrf.dab2c.executor.voice.model.currentFeatureToggles
import ru.sbrf.dab2c.executor.voice.service.api.AnalyticsPublisher
import ru.sbrf.dab2c.executor.voice.service.api.FunctionCallService

/** Implementation of [FunctionCallService] that routes function calls to IVR or backend execution. */
class FunctionCallServiceImpl(
    private val session: VoiceSession,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient,
    private val iagFunctionClient: IagFunctionClient,
    private val configuratorClient: ConfiguratorClient,
    private val analyticsPublisher: AnalyticsPublisher
) : FunctionCallService {

    private val logger = KotlinLogging.logger {}

    override suspend fun callFunction(functionCalling: FunctionCalling): FunctionCalling? {
        val state = getServingState()
        val functionName = functionCalling.functionCall.name
        val functionOptions = state.functionRegistry.functions[functionName]

        if (!currentFeatureToggles().configuratorFunctionMatch) {
            return callFunctionByPerformers(
                state,
                functionCalling,
                functionOptions?.isBackendFunction
            )
        }

        val functionConfig = getFunctionConfig(
            agentName = state.agentConfiguration.name,
            functionName = functionName
        )

        return callFunctionByConfigurator(
            state = state,
            functionCalling = functionCalling,
            functionConfig = functionConfig,
            isBackendFunction = functionOptions?.isBackendFunction
        )
    }

    private fun getServingState(): ProcessingState.Serving {
        val state = session.state.value

        check(state is ProcessingState.Serving) {
            "Expected Serving state for function calls, but was ${state::class.simpleName}"
        }

        return state
    }

    private suspend fun callFunctionByConfigurator(
        state: ProcessingState.Serving,
        functionCalling: FunctionCalling,
        functionConfig: FunctionConfig?,
        isBackendFunction: Boolean?
    ): FunctionCalling? {
        val functionName = functionCalling.functionCall.name

        return when (functionConfig?.type?.uppercase()) {
            FUNCTION_TYPE_DIVR -> {
                logger.debug { "Function '$functionName' proxied to SmartIVR by configurator" }
                functionCalling
            }

            FUNCTION_TYPE_BACKEND -> {
                executeBackendFunction(state, functionCalling, functionName)
                null
            }

            FUNCTION_TYPE_IAG -> {
                executeIagFunction(state, functionCalling, functionName, functionConfig)
                null
            }

            else -> callFunctionByPerformers(
                state,
                functionCalling,
                isBackendFunction
            )
        }
    }

    private suspend fun executeBackendFunction(
        state: ProcessingState.Serving,
        functionCalling: FunctionCalling,
        functionName: String
    ) {
        logger.debug { "Executing backend function '$functionName' by configurator (async)" }

        executeFunctionAsync(
            functionCalling = functionCalling,
            functionName = functionName,
            executorType = EXECUTOR_BACKEND,
            executor = {
                gigaVoiceAgentClient.executeFunctionCall(
                    conversationId = state.conversationId,
                    agentConfiguration = state.agentConfiguration,
                    functionCalling = functionCalling,
                    contextData = state.contextData
                )
            }
        )
    }

    private suspend fun executeIagFunction(
        state: ProcessingState.Serving,
        functionCalling: FunctionCalling,
        functionName: String,
        functionConfig: FunctionConfig
    ) {
        logger.debug { "Executing IAG function '$functionName' by configurator (async)" }

        executeFunctionAsync(
            functionCalling = functionCalling,
            functionName = functionName,
            executorType = EXECUTOR_IAG,
            executor = {
                iagFunctionClient.executeFunctionCall(
                    conversationId = state.conversationId,
                    agentConfiguration = state.agentConfiguration,
                    functionCalling = functionCalling,
                    contextData = state.contextData,
                    endpoint = functionConfig.path
                )
            }
        )
    }

    private suspend fun callFunctionByPerformers(
        state: ProcessingState.Serving,
        functionCalling: FunctionCalling,
        isBackendFunction: Boolean?
    ): FunctionCalling? {
        val functionName = functionCalling.functionCall.name

        if (isBackendFunction != true) {
            logger.debug { "Function '$functionName' proxied to IVR" }
            return functionCalling
        }

        logger.debug { "Executing backend function '$functionName' via agent (async)" }

        executeFunctionAsync(
            functionCalling = functionCalling,
            functionName = functionName,
            executorType = EXECUTOR_BACKEND,
            executor = {
                gigaVoiceAgentClient.executeFunctionCall(
                    conversationId = state.conversationId,
                    agentConfiguration = state.agentConfiguration,
                    functionCalling = functionCalling,
                    contextData = state.contextData
                )
            }
        )

        return null
    }

    private suspend fun getFunctionConfig(
        agentName: String,
        functionName: String
    ): FunctionConfig =
        runCatchingCancellable {
            configuratorClient.getFunction(agentName, functionName)[functionName]
                ?: error(
                    "Function config '$functionName' not found for agent '$agentName'"
                )
        }.getOrElse { e ->
            logger.error(e) {
                "Failed to get function config for '$functionName' from configurator"
            }
            throw e
        }

    private suspend fun executeFunctionAsync(
        functionCalling: FunctionCalling,
        functionName: String,
        executorType: String,
        executor: suspend () -> FunctionCallResult
    ) {
        val headers = currentHeaders()
        val startTime = System.currentTimeMillis()

        session.launch {
            try {
                val functionCallResult = executor()

                val elapsed = System.currentTimeMillis() - startTime
                logger.info {
                    "$executorType function '$functionName' completed in ${elapsed}ms"
                }

                analyticsPublisher.publishAnalytics(
                    functionCallResult.analytics,
                    headers.getHeaderOrNull(RequestHeader.X_REQUEST_ID)
                )

                session.callbackChannels.downstream.send(
                    gigaVoiceRequest { functionResult = functionCallResult.result }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: ClosedSendChannelException) {
                logger.debug(e) { "Channel closed, session ended before function '$functionName' completed" }
            } catch (e: Exception) {
                sendFunctionError(functionCalling, functionName, startTime, e)
            }
        }
    }

    private suspend fun sendFunctionError(
        functionCalling: FunctionCalling,
        functionName: String,
        startTime: Long,
        e: Exception
    ) {
        val elapsed = System.currentTimeMillis() - startTime
        logger.error { "Failed to execute backend function '$functionName' after ${elapsed}ms: ${e.message}" }

        val escapedMessage = e.message?.replace("\"", "\\\"") ?: "Function execution failed"
        val errorContent = """{"error":{"code":500,"message":"$escapedMessage"}}"""

        try {
            session.callbackChannels.downstream.send(
                gigaVoiceRequest {
                    functionResult = functionResult {
                        this.content = errorContent
                        this.functionName = functionCalling.functionCall.name
                    }
                }
            )
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: ClosedSendChannelException) {
            logger.debug(ex) { "Channel closed, session ended before error for '$functionName' could be sent" }
        }
    }

    private companion object {
        private const val FUNCTION_TYPE_DIVR = "DIVR"
        private const val FUNCTION_TYPE_BACKEND = "BACKEND"
        private const val FUNCTION_TYPE_IAG = "IAG"
        private const val EXECUTOR_BACKEND = "backend"
        private const val EXECUTOR_IAG = "iag"
    }
}
