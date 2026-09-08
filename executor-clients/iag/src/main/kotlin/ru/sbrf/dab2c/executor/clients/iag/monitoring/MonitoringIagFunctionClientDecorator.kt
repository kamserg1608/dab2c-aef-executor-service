package ru.sbrf.dab2c.executor.clients.iag.monitoring

import ru.sbrf.dab2c.executor.clients.common.model.ClientMetric
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.clients.iag.api.IagFunctionClient
import ru.sbrf.dab2c.executor.clients.iag.api.IagFunctionClient.Companion.FUNCTION_CALL_ENDPOINT
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.monitoring.service.api.HttpCallDescriptor
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import ru.sbrf.dab2c.executor.library.monitoring.service.api.monitorHttpCall

/**
 * Decorator that records HTTP integration metrics for IAG function calls.
 */
class MonitoringIagFunctionClientDecorator(
    private val delegate: IagFunctionClient,
    private val metricFactory: MetricFactory
) : IagFunctionClient {

    override suspend fun executeFunctionCall(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCalling,
        contextData: DialogContext,
        endpoint: String?
    ): FunctionCallResult {
        val functionCallEndpoint = endpoint ?: FUNCTION_CALL_ENDPOINT

        return metricFactory.monitorHttpCall(
            timerMetric = ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
            counterMetric = ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
            descriptor = HttpCallDescriptor(
                destinationService = IAG,
                endpoint = functionCallEndpoint
            )
        ) {
            delegate.executeFunctionCall(
                conversationId = conversationId,
                agentConfiguration = agentConfiguration,
                functionCalling = functionCalling,
                contextData = contextData,
                endpoint = functionCallEndpoint
            )
        }
    }

    /** Destination service identifier. */
    companion object {
        private const val IAG = "iag"
    }
}
