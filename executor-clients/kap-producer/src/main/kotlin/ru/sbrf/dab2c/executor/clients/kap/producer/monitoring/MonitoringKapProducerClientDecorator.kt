package ru.sbrf.dab2c.executor.clients.kap.producer.monitoring

import kotlinx.coroutines.CancellationException
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.configuration.properties.KapProducerConfigurationProperties
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogEnvelope
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory

/**
 * Decorator that records KAP producer metrics for dialog and agent analytics publishing.
 */
class MonitoringKapProducerClientDecorator(
    private val delegate: KapProducerClient,
    private val properties: KapProducerConfigurationProperties,
    private val metricFactory: MetricFactory
) : KapProducerClient {

    override suspend fun publishDialog(dialog: DialogEnvelope) {
        monitorPublish(
            messageType = KapProducerMetricTags.DIALOG,
            topic = properties.dialogsTopic
        ) {
            delegate.publishDialog(dialog)
        }
    }

    override suspend fun publishAgentAnalytics(analytics: AgentAnalyticsEnvelope) {
        monitorPublish(
            messageType = KapProducerMetricTags.AGENT_ANALYTICS,
            topic = properties.agentsTopic
        ) {
            delegate.publishAgentAnalytics(analytics)
        }
    }

    private suspend fun monitorPublish(
        messageType: String,
        topic: String,
        block: suspend () -> Unit
    ) {
        val baseTags = baseTags(messageType, topic)
        val startTime = System.nanoTime()

        incrementPrepared(baseTags)

        try {
            block()
            recordSuccess(baseTags, startTime)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recordException(baseTags, startTime, e)
            throw e
        }
    }

    private fun baseTags(
        messageType: String,
        topic: String
    ): Map<String, String> =
        mapOf(
            KapProducerMetricTags.MESSAGE_TYPE to messageType,
            KapProducerMetricTags.TOPIC to topic
        )

    private suspend fun incrementPrepared(tags: Map<String, String>) {
        metricFactory.incrementCounter(
            metric = KapProducerMetric.ANALYTICS_PREPARED_MESSAGES_TOTAL,
            tags = tags
        )
    }

    private suspend fun recordSuccess(
        tags: Map<String, String>,
        startTime: Long
    ) {
        val successTags = tags + (KapProducerMetricTags.STATUS to KapProducerMetricTags.SUCCESS)

        metricFactory.incrementCounter(
            metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_TOTAL,
            tags = successTags
        )
        recordDuration(successTags, startTime)
    }

    private suspend fun recordException(
        tags: Map<String, String>,
        startTime: Long,
        e: Exception
    ) {
        metricFactory.incrementCounter(
            metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_EXCEPTIONS_TOTAL,
            tags = tags + mapOf(
                KapProducerMetricTags.STATUS to KapProducerMetricTags.EXCEPTION,
                KapProducerMetricTags.EXCEPTION to e::class.simpleName.orEmpty()
            )
        )
        recordDuration(
            tags = tags + (KapProducerMetricTags.STATUS to KapProducerMetricTags.EXCEPTION),
            startTime = startTime
        )
    }

    private suspend fun recordDuration(
        tags: Map<String, String>,
        startTime: Long
    ) {
        metricFactory.recordDuration(
            metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_DURATION_SECONDS,
            durationNs = System.nanoTime() - startTime,
            tags = tags
        )
    }
}
