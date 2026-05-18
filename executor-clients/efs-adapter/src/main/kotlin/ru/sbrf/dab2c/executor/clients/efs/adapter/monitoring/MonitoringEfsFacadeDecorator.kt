package ru.sbrf.dab2c.executor.clients.efs.adapter.monitoring

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.common.model.ClientMetric
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.AUDIT_EVENT_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.FUNCTION_LIST_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.PERSON_INFO_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.READ_DATA_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.REST_AGENT_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.RETRIEVE_PARAMS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.SESSION_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.WRITE_DATA_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.Parameter
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.configuration.FunctionListResponse
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.domain.session.SdsSection
import ru.sbrf.dab2c.executor.library.audit.model.AuditEvent
import ru.sbrf.dab2c.executor.library.monitoring.service.api.HttpCallDescriptor
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import ru.sbrf.dab2c.executor.library.monitoring.service.api.monitorHttpCall

/**
 * Monitoring decorator that records HTTP integration metrics for all EFS Adapter calls.
 */
@Service
@Primary
class MonitoringEfsFacadeDecorator(
    @Qualifier("efsFacadeImpl") private val delegate: EfsFacade,
    private val metricFactory: MetricFactory
) : EfsFacade {

    override suspend fun sendEvent(event: AuditEvent) =
        monitorCall(AUDIT_EVENT_ENDPOINT) {
            delegate.sendEvent(event)
        }

    override suspend fun getRestAgentConfig(agentName: String): AgentConfiguration =
        monitorCall(REST_AGENT_ENDPOINT) {
            delegate.getRestAgentConfig(agentName)
        }

    override suspend fun getDaSessionCommon(): DaSessionCommon =
        monitorCall(SESSION_ENDPOINT) {
            delegate.getDaSessionCommon()
        }

    override suspend fun getFunctionCall(
        agentName: String,
        modality: String
    ): FunctionListResponse =
        monitorCall(FUNCTION_LIST_ENDPOINT) {
            delegate.getFunctionCall(agentName, modality)
        }

    override suspend fun getParameter(name: String): Parameter =
        monitorCall(RETRIEVE_PARAMS_ENDPOINT) {
            delegate.getParameter(name)
        }

    override suspend fun getParameters(names: List<String>): Map<String, Parameter> =
        monitorCall(RETRIEVE_PARAMS_ENDPOINT) {
            delegate.getParameters(names)
        }

    override suspend fun getPersonInfo(): DaSessionUserInfo =
        monitorCall(PERSON_INFO_ENDPOINT) {
            delegate.getPersonInfo()
        }

    override suspend fun readData(sections: List<SdsSection>): List<SdsSection> =
        monitorCall(READ_DATA_ENDPOINT) {
            delegate.readData(sections)
        }

    override suspend fun writeData(sections: List<SdsSection>) =
        monitorCall(WRITE_DATA_ENDPOINT) {
            delegate.writeData(sections)
        }

    private suspend fun <T> monitorCall(
        endpoint: String,
        block: suspend () -> T
    ): T = metricFactory.monitorHttpCall(
        timerMetric = ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
        counterMetric = ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
        descriptor = HttpCallDescriptor(
            destinationService = EFS_ADAPTER,
            endpoint = endpoint
        ),
        block = block
    )

    /** Destination service identifier. */
    companion object {
        private const val EFS_ADAPTER = "efs-adapter"
    }
}
