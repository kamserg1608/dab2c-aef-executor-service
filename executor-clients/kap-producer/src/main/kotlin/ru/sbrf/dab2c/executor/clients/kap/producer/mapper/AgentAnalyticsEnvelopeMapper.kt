@file:Suppress("UndocumentedPublicFunction")

package ru.sbrf.dab2c.executor.clients.kap.producer.mapper

import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import java.util.Locale

/**
 * Input data for mapping agent analytics to AgentAnalyticsEnvelope.
 */
data class AgentAnalyticsData(
    val envelopeId: String,
    val timestamp: Long,
    val conversationId: String,
    val requestId: String?,
    val dataVersion: String,
    val data: String,
    val daSessionInfo: DaSessionInfo,
    val agentConfiguration: AgentConfiguration
)

/**
 * Mapper for converting agent analytics data to KAP AgentAnalyticsEnvelope.
 */
object AgentAnalyticsEnvelopeMapper {

    private const val ANALYTICS_VERSION = "1.1.0"
    private const val MEGABYTE_DIVISOR = 1_000_000.0

    fun toAgentAnalyticsEnvelope(data: AgentAnalyticsData): AgentAnalyticsEnvelope {
        val meta = data.daSessionInfo.meta
        val common = data.daSessionInfo.common
        val sizeInMb = calculateSizeInMb(data.data)

        return AgentAnalyticsEnvelope(
            version = ANALYTICS_VERSION,
            id = data.envelopeId,
            date = data.timestamp,
            sessionId = meta.sessionId,
            conversationId = data.conversationId,
            ucpId = meta.ucpId,
            block = common.block,
            channel = common.channel,
            agentName = data.agentConfiguration.name,
            dataVersion = data.dataVersion,
            agentCi = data.agentConfiguration.functionalSubsystemCi,
            size = sizeInMb,
            requestId = data.requestId,
            appSource = common.appSource.takeIfNotBlank(),
            platform = common.platform.takeIfNotBlank(),
            data = data.data
        )
    }

    private fun calculateSizeInMb(dataJson: String): String {
        val sizeInBytes = dataJson.toByteArray(Charsets.UTF_8).size
        val sizeInMb = sizeInBytes / MEGABYTE_DIVISOR
        return String.format(Locale.US, "%.6f", sizeInMb)
    }

    private fun String.takeIfNotBlank(): String? = takeIf { it.isNotBlank() }
}
