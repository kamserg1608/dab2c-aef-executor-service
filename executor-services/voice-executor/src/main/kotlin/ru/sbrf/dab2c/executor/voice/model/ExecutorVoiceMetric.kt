package ru.sbrf.dab2c.executor.voice.model

import ru.sbrf.dab2c.executor.library.monitoring.service.api.Metric

/**
 * Metrics for voice executor.
 */
enum class ExecutorVoiceMetric(
    override var metricName: String
) : Metric {
    GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL("grpc_incoming_from_initiator_chunks_total"),
    GRPC_OUTGOING_FROM_INITIATOR_CHUNKS_TOTAL("grpc_outgoing_to_initiator_chunks_total"),
    GRPC_OUTGOING_FROM_GIGAVOICE_CHUNKS_TOTAL("grpc_outgoing_to_gigavoice_chunks_total"),
    GRPC_INCOMING_FROM_GIGAVOICE_CHUNKS_TOTAL("grpc_incoming_from_gigavoice_chunks_total"),
    GRPC_RESPONSE_TOTAL_TOKENS("grpc_response_total_tokens"),
    GRPC_CONNECTIONS_TOTAL("grpc_connections_total"),
    GRPC_CONNECTIONS_ACTIVE("grpc_connections_active"),
    GRPC_CONNECTIONS_DURATION("grpc_connections_duration"),
    GRPC_CONNECTIONS_TTFB_SECONDS("grpc_connections_ttfb_seconds")
}
