package ru.sbrf.dab2c.executor.clients.kap.producer.api

import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogEnvelope

/**
 * Client interface for publishing messages to KAP Kafka topics.
 */
interface KapProducerClient {

    /**
     * Publish a dialog message pair to the dialogs topic.
     */
    suspend fun publishDialog(dialog: DialogEnvelope)

    /**
     * Publish agent analytics data to the agents topic.
     */
    suspend fun publishAgentAnalytics(analytics: AgentAnalyticsEnvelope)
}
