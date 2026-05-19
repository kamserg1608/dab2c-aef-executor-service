package ru.sbrf.dab2c.executor.clients.configurator.monitoring

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.common.model.ClientMetric
import ru.sbrf.dab2c.executor.clients.configurator.api.ConfiguratorFunctionFacade
import ru.sbrf.dab2c.executor.clients.configurator.api.ConfiguratorFunctionFacade.Companion.FUNCTION_LIST_ENDPOINT
import ru.sbrf.dab2c.executor.domain.configuration.FunctionListResponse
import ru.sbrf.dab2c.executor.library.monitoring.service.api.HttpCallDescriptor
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import ru.sbrf.dab2c.executor.library.monitoring.service.api.monitorHttpCall

/**
 * Monitoring decorator that records HTTP integration metrics for all Configurator calls.
 */
@Service
@Primary
class MonitoringConfiguratorFunctionFacadeDecorator(
    @Qualifier("configuratorFacadeImpl") private val delegate: ConfiguratorFunctionFacade,
    private val metricFactory: MetricFactory
) : ConfiguratorFunctionFacade {

    override suspend fun getFunctionCall(
        agentName: String,
        modality: String
    ): FunctionListResponse =
        monitorCall(FUNCTION_LIST_ENDPOINT) {
            delegate.getFunctionCall(agentName, modality)
        }

    private suspend fun <T> monitorCall(
        endpoint: String,
        block: suspend () -> T
    ): T = metricFactory.monitorHttpCall(
        timerMetric = ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
        counterMetric = ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
        descriptor = HttpCallDescriptor(
            destinationService = CONFIGURATOR,
            endpoint = endpoint
        ),
        block = block
    )

    /** Destination service identifier. */
    companion object {
        private const val CONFIGURATOR = "configurator"
    }
}
