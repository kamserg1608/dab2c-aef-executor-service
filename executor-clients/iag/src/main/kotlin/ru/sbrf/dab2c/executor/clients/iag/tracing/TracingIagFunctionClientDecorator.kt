package ru.sbrf.dab2c.executor.clients.iag.tracing

import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.clients.iag.api.IagFunctionClient
import ru.sbrf.dab2c.executor.clients.iag.api.IagFunctionClient.Companion.FUNCTION_CALL_ENDPOINT
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.tracing.facade.AefHttpOutgoingRequestTracing

/** Wraps [IagFunctionClient] HTTP calls in `output_request` spans with real bodies. */
class TracingIagFunctionClientDecorator(
    private val delegate: IagFunctionClient,
    private val aefTracing: AefHttpOutgoingRequestTracing
) : IagFunctionClient {
    override suspend fun executeFunctionCall(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCalling,
        contextData: DialogContext,
        endpoint: String?
    ): FunctionCallResult = aefTracing.trace(
        spanName = "iag $FUNCTION_CALL_ENDPOINT",
        path = FUNCTION_CALL_ENDPOINT,
        request = mapOf(
            "conversationId" to conversationId,
            "agentConfiguration" to agentConfiguration,
            "functionCalling" to functionCalling,
            "contextData" to contextData,
            "endpoint" to endpoint
        )
    ) {
        delegate.executeFunctionCall(
            conversationId,
            agentConfiguration,
            functionCalling,
            contextData,
            endpoint
        )
    }
}
