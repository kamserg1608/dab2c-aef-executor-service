package ru.sbrf.dab2c.executor.clients.configurator.tracing

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.configurator.api.DirectConfiguratorClient
import ru.sbrf.dab2c.executor.clients.configurator.api.DirectConfiguratorClient.Companion.FUNCTION_LIST_ENDPOINT
import ru.sbrf.dab2c.executor.domain.configuration.FunctionList
import ru.sbrf.dab2c.executor.library.tracing.facade.AefHttpOutgoingRequestTracing

/**
 * AEF tracing decorator that sends `output_request` span for all Configurator calls.
 */
@Service
@Primary
class TracingDirectConfiguratorClientDecorator(
    @Qualifier("monitoringConfiguratorClientDecorator") private val delegate: DirectConfiguratorClient,
    private val aefTracing: AefHttpOutgoingRequestTracing
) : DirectConfiguratorClient {
    override suspend fun fetchFunctionRegistry(agentName: String, modality: String): FunctionList =
        aefTracing.trace(
            spanName = "configurator $FUNCTION_LIST_ENDPOINT",
            path = FUNCTION_LIST_ENDPOINT,
            request = mapOf(
                "agentName" to agentName,
                "modality" to modality
            )
        ) {
            delegate.fetchFunctionRegistry(agentName, modality)
        }
}
