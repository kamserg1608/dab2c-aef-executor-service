package ru.sbrf.dab2c.executor.clients.kap.producer.monitoring

import ru.sbrf.dab2c.executor.library.monitoring.service.api.Metric

/**
 * Metrics emitted by KAP producer publishing operations.
 */
enum class KapProducerMetric(
    override val metricName: String
) : Metric {
    ANALYTICS_PUBLISHED_MESSAGES_DURATION_SECONDS("analytics_published_messages_duration_seconds"),
    ANALYTICS_PUBLISHED_MESSAGES_TOTAL("analytics_published_messages_total"),
    ANALYTICS_PUBLISHED_MESSAGES_EXCEPTIONS_TOTAL("analytics_published_messages_exceptions_total"),
    ANALYTICS_PREPARED_MESSAGES_TOTAL("analytics_prepared_messages_total")
}
