package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ClosedSendChannelException
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.gigavoice.mapper.toProto
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaVoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.FunctionResultData
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.VoiceSession
import ru.sbrf.dab2c.executor.voice.service.api.AnalyticsPublisher
import ru.sbrf.dab2c.executor.voice.service.api.FunctionCallService

/** Implementation of [FunctionCallService] that routes function calls to IVR or backend execution. */
class FunctionCallServiceImpl(
    private val session: VoiceSession,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient,
    private val analyticsPublisher: AnalyticsPublisher
) : FunctionCallService {

    private val logger = KotlinLogging.logger {}

    override suspend fun callFunction(functionCalling: FunctionCallingData): FunctionCallingData? {
        val state = session.state.value
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
        executeBackendFunctionAsync(state, functionCalling)
        return null
    }

    @Suppress("LongMethod")
    private suspend fun executeBackendFunctionAsync(
        state: ProcessingState.Serving,
        functionCalling: FunctionCallingData
    ) {
        val headers = currentHeaders()
        val functionName = functionCalling.functionCall.name
        val startTime = System.currentTimeMillis()

        session.launch {
            try {
                val functionCallResult = gigaVoiceAgentClient.executeFunctionCall(
                    conversationId = state.conversationId,
                    agentConfiguration = state.agentConfiguration,
                    functionCalling = functionCalling,
                    contextData = state.contextData
                )

                val elapsed = System.currentTimeMillis() - startTime
                logger.info { "Backend function '$functionName' completed in ${elapsed}ms" }

                analyticsPublisher.publishAnalytics(
                    functionCallResult.analytics,
                    headers.getHeaderOrNull(RequestHeader.X_REQUEST_ID)
                )

                session.callbackChannels.downstream.send(
                    gigaVoiceRequest { functionResult = functionCallResult.result.toProto() }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: ClosedSendChannelException) {
                logger.debug(e) { "Channel closed, session ended before function '$functionName' completed" }
            } catch (e: Exception) {
                val elapsed = System.currentTimeMillis() - startTime
                logger.error { "Failed to execute backend function '$functionName' after ${elapsed}ms: ${e.message}" }
                val escapedMessage = e.message?.replace("\"", "\\\"") ?: "Function execution failed"
                val errorContent = """{"error":{"code":500,"message":"$escapedMessage"}}"""
                try {
                    session.callbackChannels.downstream.send(
                        gigaVoiceRequest {
                            functionResult = FunctionResultData(
                                content = errorContent,
                                functionName = functionCalling.functionCall.name
                            ).toProto()
                        }
                    )
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: ClosedSendChannelException) {
                    logger.debug(ex) { "Channel closed, session ended before error for '$functionName' could be sent" }
                }
            }
        }
    }
}
