@file:Suppress("StringLiteralDuplication")

package ru.sbrf.dab2c.executor.clients.efs.adapter.tracing

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.FUNCTION_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.PERSON_INFO_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.REST_AGENT_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.SESSION_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.Parameter
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.configuration.FunctionConfig
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.domain.session.SdsSection
import ru.sbrf.dab2c.executor.library.audit.model.AuditEvent
import ru.sbrf.dab2c.executor.library.tracing.facade.AefHttpOutgoingRequestTracing

/**
 * AEF tracing decorator that sends `output_request` span for all EFS Adapter calls.
 */
@Service
@Primary
class TracingEfsFacadeDecorator(
    @Qualifier("monitoringEfsFacadeDecorator") private val delegate: EfsFacade,
    private val aefTracing: AefHttpOutgoingRequestTracing
) : EfsFacade {
    override suspend fun sendEvent(event: AuditEvent) = delegate.sendEvent(event)

    override suspend fun getRestAgentConfig(agentName: String): AgentConfiguration =
        aefTracing.trace(
            spanName = "sidecar $REST_AGENT_ENDPOINT",
            path = REST_AGENT_ENDPOINT,
            request = mapOf(
                "agentName" to agentName
            )
        ) {
            delegate.getRestAgentConfig(agentName)
        }

    override suspend fun getDaSessionCommon(): DaSessionCommon =
        aefTracing.trace(
            spanName = "sidecar $SESSION_ENDPOINT",
            path = SESSION_ENDPOINT,
            request = emptyMap<String, Any>()
        ) {
            delegate.getDaSessionCommon()
        }

    override suspend fun getFunction(agentName: String, functionName: String): FunctionConfig =
        aefTracing.trace(
            spanName = "sidecar $FUNCTION_ENDPOINT",
            path = FUNCTION_ENDPOINT,
            request = mapOf(
                "agentName" to agentName,
                "functionName" to functionName
            )
        ) {
            delegate.getFunction(agentName, functionName)
        }

    override suspend fun getParameter(name: String): Parameter = delegate.getParameter(name)

    override suspend fun getParameters(names: List<String>): Map<String, Parameter> = delegate.getParameters(names)

    override suspend fun getPersonInfo(): DaSessionUserInfo =
        aefTracing.trace(
            spanName = "sidecar $PERSON_INFO_ENDPOINT",
            path = PERSON_INFO_ENDPOINT,
            request = emptyMap<String, Any>()
        ) {
            delegate.getPersonInfo()
        }

    override suspend fun readData(sections: List<SdsSection>): List<SdsSection> = delegate.readData(sections)

    override suspend fun writeData(sections: List<SdsSection>) = delegate.writeData(sections)
}
