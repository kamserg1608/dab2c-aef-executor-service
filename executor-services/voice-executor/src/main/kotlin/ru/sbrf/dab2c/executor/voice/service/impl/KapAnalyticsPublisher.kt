package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.mapper.AgentAnalyticsEnvelopeMapper
import ru.sbrf.dab2c.executor.clients.kap.producer.mapper.AnalyticsTurnData
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.library.context.currentSessionInfo
import ru.sbrf.dab2c.executor.voice.service.api.AnalyticsPublisher
import java.util.UUID

/** Publishes agent analytics events to KAP. */
class KapAnalyticsPublisher(
    private val kapProducerClient: KapProducerClient
) : AnalyticsPublisher {

    override suspend fun publishAnalytics(
        analytics: List<AgentAnalytics>,
        requestId: String?,
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        assistantMessageId: String
    ) {
        if (analytics.isEmpty()) {
            return
        }

        logger.info { "Publishing ${analytics.size} analytics event(s)" }

        val resolvedRequestId = requestId ?: currentHeaders().getHeaderOrNull(RequestHeader.X_REQUEST_ID)
        val daSessionInfo = currentSessionInfo()

        analytics.forEach { item ->
            val turnData = AnalyticsTurnData(
                envelopeId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis() / MILLIS_TO_SECONDS,
                daSessionInfo = daSessionInfo,
                conversationId = conversationId,
                requestId = resolvedRequestId,
                messageId = assistantMessageId,
                agentName = agentConfiguration.name,
                agentCi = agentConfiguration.functionalSubsystemCi,
                dataVersion = item.dataVersion,
                data = item.data
            )
            val envelope = AgentAnalyticsEnvelopeMapper.toAgentAnalyticsEnvelope(turnData)
            logger.debug { "Analytics envelope content: $envelope" }
            kapProducerClient.publishAgentAnalytics(envelope)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val MILLIS_TO_SECONDS = 1000L
    }
}
