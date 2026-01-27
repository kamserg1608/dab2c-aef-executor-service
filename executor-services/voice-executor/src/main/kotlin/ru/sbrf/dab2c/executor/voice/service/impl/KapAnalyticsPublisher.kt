package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.mapper.AgentAnalyticsEnvelopeMapper
import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics
import ru.sbrf.dab2c.executor.voice.service.api.AnalyticsPublisher
import java.util.UUID

/**
 * Publishes agent analytics to KAP (Kafka Analytics Platform).
 */
class KapAnalyticsPublisher(
    private val kapProducerClient: KapProducerClient
) : AnalyticsPublisher {

    override suspend fun publishAnalytics(analytics: List<AgentAnalytics>, requestId: String?) {
        if (analytics.isEmpty()) {
            return
        }

        logger.info { "Publishing ${analytics.size} analytics event(s)" }

        analytics.forEach { analyticsItem ->
            val envelope = AgentAnalyticsEnvelopeMapper.toAgentAnalyticsEnvelope(
                envelopeId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis() / MILLIS_TO_SECONDS,
                data = analyticsItem.data
            )
            logger.debug { "Publishing analytics: id=${envelope.id}" }
            kapProducerClient.publishAgentAnalytics(envelope)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val MILLIS_TO_SECONDS = 1000L
    }
}
