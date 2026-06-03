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
        monitorPublish(destination = properties.dialogsTopic) {
            delegate.publishDialog(dialog)
        }
    }

    override suspend fun publishAgentAnalytics(analytics: AgentAnalyticsEnvelope) {
        monitorPublish(destination = properties.agentsTopic) {
            delegate.publishAgentAnalytics(analytics)
        }
    }

    private suspend fun monitorPublish(
        destination: String,
        block: suspend () -> Unit
    ) {
        val startTime = System.nanoTime()

        incrementPrepared(destination)

        try {
            block()
            recordSuccess(destination, startTime)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recordException(destination, startTime, e)
            throw e
        }
    }

    private suspend fun incrementPrepared(destination: String) {
        metricFactory.incrementCounter(
            metric = KapProducerMetric.ANALYTICS_PREPARED_MESSAGES_TOTAL,
            tags = mapOf(
                KapProducerMetricTags.DESTINATION to destination,
                KapProducerMetricTags.STATUS to KapProducerMetricTags.SUCCESS
            )
        )
    }

    private suspend fun recordSuccess(
        destination: String,
        startTime: Long
    ) {
        metricFactory.incrementCounter(
            metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_TOTAL,
            tags = mapOf(
                KapProducerMetricTags.DESTINATION to destination,
                KapProducerMetricTags.STATUS to KapProducerMetricTags.SUCCESS
            )
        )
        recordDuration(destination, startTime)
    }

    private suspend fun recordException(
        destination: String,
        startTime: Long,
        exception: Exception
    ) {
        metricFactory.incrementCounter(
            metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_EXCEPTIONS_TOTAL,
            tags = mapOf(
                KapProducerMetricTags.DESTINATION to destination,
                KapProducerMetricTags.EXCEPTION_TYPE to exception::class.simpleName.orEmpty()
            )
        )
        recordDuration(destination, startTime)
    }

    private suspend fun recordDuration(
        destination: String,
        startTime: Long
    ) {
        metricFactory.recordDuration(
            metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_DURATION_SECONDS,
            durationNs = System.nanoTime() - startTime,
            tags = mapOf(KapProducerMetricTags.DESTINATION to destination)
        )
    }
}
