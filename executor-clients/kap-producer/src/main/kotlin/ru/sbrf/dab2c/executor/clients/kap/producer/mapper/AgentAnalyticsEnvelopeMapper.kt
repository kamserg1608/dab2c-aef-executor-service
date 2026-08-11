@file:Suppress("UndocumentedPublicFunction")

package ru.sbrf.dab2c.executor.clients.kap.producer.mapper

import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsData
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Input data for mapping agent analytics to AgentAnalyticsEnvelope.
 */
data class AnalyticsTurnData(
    val envelopeId: String,
    val timestamp: Long,
    val daSessionInfo: DaSessionInfo,
    val conversationId: String,
    val requestId: String?,
    val messageId: String,
    val agentName: String,
    val agentCi: String,
    val dataVersion: String,
    val data: String
)

/**
 * Mapper for converting agent analytics data to KAP AgentAnalyticsEnvelope.
 */
object AgentAnalyticsEnvelopeMapper {

    private const val ANALYTICS_VERSION = "1.0.0"

    fun toAgentAnalyticsEnvelope(turnData: AnalyticsTurnData): AgentAnalyticsEnvelope {
        val analyticsData = AgentAnalyticsData(
            sessionId = turnData.daSessionInfo.meta.sessionId,
            conversationId = turnData.conversationId,
            requestId = turnData.requestId ?: "",
            messageId = turnData.messageId,
            ucpId = turnData.daSessionInfo.meta.ucpId ?: "",
            block = turnData.daSessionInfo.common.block ?: "",
            channel = turnData.daSessionInfo.common.channel,
            appSource = turnData.daSessionInfo.common.appSource ?: "",
            platform = turnData.daSessionInfo.common.platform ?: "",
            agentName = turnData.agentName,
            agentCi = turnData.agentCi,
            size = turnData.data.toByteArray(Charsets.UTF_8).size.toString(),
            dataVersion = turnData.dataVersion,
            data = turnData.data
        )

        return AgentAnalyticsEnvelope(
            version = ANALYTICS_VERSION,
            id = turnData.envelopeId,
            date = turnData.timestamp,
            data = ObjectMappers.MAPPER.writeValueAsString(analyticsData)
        )
    }
}
