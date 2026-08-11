package ru.sbrf.dab2c.executor.clients.kap.producer.impl

import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogEnvelope

/**
 * No-op implementation of KapProducerClient for when KAP publishing is disabled.
 */
object NoopKapProducerClient : KapProducerClient {

    override suspend fun publishDialog(dialog: DialogEnvelope) {
        // No-op: KAP publishing is disabled
    }

    override suspend fun publishAgentAnalytics(analytics: AgentAnalyticsEnvelope) {
        // No-op: KAP publishing is disabled
    }
}
