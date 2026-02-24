package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.StateFlow
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.mapper.AgentAnalyticsEnvelopeMapper
import ru.sbrf.dab2c.executor.clients.kap.producer.mapper.AnalyticsTurnData
import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.library.context.currentSessionInfo
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.AnalyticsPublisher
import java.util.UUID

/** Publishes agent analytics events to KAP. */
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

        val headers = currentHeaders()
        val daSessionInfo = currentSessionInfo()

        analytics.forEach { analyticsItem ->
            val turnData = AnalyticsTurnData(
                envelopeId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis() / MILLIS_TO_SECONDS,
                daSessionInfo = daSessionInfo,
                conversationId = state.conversationId,
                requestId = requestId ?: headers.getHeaderOrNull(RequestHeader.X_REQUEST_ID),
                agentName = state.agentConfiguration.name,
                agentCi = state.agentConfiguration.functionalSubsystemCi,
                dataVersion = analyticsItem.dataVersion,
                data = analyticsItem.data
            )

            val envelope = AgentAnalyticsEnvelopeMapper.toAgentAnalyticsEnvelope(turnData)
            logger.debug { "Publishing analytics: id=${envelope.id}" }
            kapProducerClient.publishAgentAnalytics(envelope)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val MILLIS_TO_SECONDS = 1000L
    }
}
