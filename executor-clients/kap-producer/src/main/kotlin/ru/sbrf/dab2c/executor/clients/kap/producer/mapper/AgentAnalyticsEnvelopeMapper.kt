@file:Suppress("UndocumentedPublicFunction")

package ru.sbrf.dab2c.executor.clients.kap.producer.mapper

import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope

/**
 * Mapper for converting agent analytics data to KAP AgentAnalyticsEnvelope.
 */
object AgentAnalyticsEnvelopeMapper {

    private const val ANALYTICS_VERSION = "1.2.0"

    fun toAgentAnalyticsEnvelope(
        envelopeId: String,
        timestamp: Long,
        data: String
    ): AgentAnalyticsEnvelope = AgentAnalyticsEnvelope(
        version = ANALYTICS_VERSION,
        id = envelopeId,
        date = timestamp,
        data = data
    )
}
