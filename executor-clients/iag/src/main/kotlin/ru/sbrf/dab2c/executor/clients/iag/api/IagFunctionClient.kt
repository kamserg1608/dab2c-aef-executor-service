package ru.sbrf.dab2c.executor.clients.iag.api

import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Context
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration

/**
 * Client for IAG function call API.
 */
interface IagFunctionClient {

    /**
     * Executes backend function call through IAG.
     */
    suspend fun executeFunctionCall(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCalling,
        contextData: Context,
        endpoint: String? = null
    ): FunctionCallResult
}
