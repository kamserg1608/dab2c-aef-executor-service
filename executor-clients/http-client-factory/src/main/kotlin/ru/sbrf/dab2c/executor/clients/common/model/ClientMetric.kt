package ru.sbrf.dab2c.executor.clients.common.model

import ru.sbrf.dab2c.executor.library.monitoring.service.api.Metric

/**
 * Http метрики для клиента.
 *
 * - [HTTP_INTEGRATION_REQUEST_TOTAL] — счётчик HTTP-запросов к интеграциям.
 * - [HTTP_INTEGRATION_REQUEST_DURATION_SECONDS] — время выполнения HTTP-запросов к интеграциям (в секундах).
 */
enum class ClientMetric(
    override val metricName: String
) : Metric {
    HTTP_INTEGRATION_REQUEST_TOTAL("http_integration_requests_total"),
    HTTP_INTEGRATION_REQUEST_DURATION_SECONDS("http_integration_request_duration_seconds")
}
