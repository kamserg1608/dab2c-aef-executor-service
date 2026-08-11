package ru.sbrf.dab2c.executor.voice.service.api

import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics

/**
 * Service for publishing agent analytics to KAP.
 */
interface AnalyticsPublisher {

    /**
     * Publishes agent analytics events to KAP.
     * Each AgentAnalytics item is published as a separate event.
     */
    suspend fun publishAnalytics(analytics: List<AgentAnalytics>, requestId: String?)
}
