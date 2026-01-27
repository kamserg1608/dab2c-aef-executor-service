package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.StateFlow
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.mapper.AgentAnalyticsData
import ru.sbrf.dab2c.executor.clients.kap.producer.mapper.AgentAnalyticsEnvelopeMapper
import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.AnalyticsPublisher
import ru.sbrf.dab2c.executor.voice.util.extensions.currentRequestMetadata
import java.util.UUID

/**
 * Publishes agent analytics to KAP (Kafka Analytics Platform).
 */
class KapAnalyticsPublisher(
    private val kapProducerClient: KapProducerClient,
    private val processingState: StateFlow<ProcessingState>
) : AnalyticsPublisher {

    override suspend fun publishAnalytics(analytics: List<AgentAnalytics>, requestId: String?) {
        if (analytics.isEmpty()) {
            return
        }

        val state = processingState.value
        if (state !is ProcessingState.Serving) {
            logger.warn { "Cannot publish analytics - not in Serving state, current state: ${state::class.simpleName}" }
            return
        }

        logger.info { "Publishing ${analytics.size} analytics event(s)" }

        analytics.forEach { analyticsItem ->
            publishSingleAnalytics(analyticsItem, requestId, state)
        }
    }

    private suspend fun publishSingleAnalytics(
        analyticsItem: AgentAnalytics,
        requestId: String?,
        state: ProcessingState.Serving
    ) {
        val metadata = currentRequestMetadata()
        val analyticsData = AgentAnalyticsData(
            envelopeId = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis() / MILLIS_TO_SECONDS,
            conversationId = state.conversationId,
            requestId = requestId,
            dataVersion = analyticsItem.dataVersion,
            data = analyticsItem.data,
            daSessionInfo = metadata.daSessionInfo,
            agentConfiguration = state.agentConfiguration
        )

        val envelope = AgentAnalyticsEnvelopeMapper.toAgentAnalyticsEnvelope(analyticsData)

        logger.debug { "Publishing analytics: id=${envelope.id}, agentName=${envelope.agentName}" }

        kapProducerClient.publishAgentAnalytics(envelope)
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val MILLIS_TO_SECONDS = 1000L
    }
}
