package ru.sbrf.dab2c.executor.voice.model

import ru.sbrf.dab2c.executor.library.monitoring.service.api.Metric

/**
 * Metrics for voice executor.
 */
enum class ExecutorVoiceMetric(
    override val metricName: String
) : Metric {
    GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL("grpc_incoming_from_initiator_chunks_total"),
    GRPC_OUTGOING_TO_INITIATOR_CHUNKS_TOTAL("grpc_outgoing_to_initiator_chunks_total"),
    GRPC_OUTGOING_TO_GIGAVOICE_CHUNKS_TOTAL("grpc_outgoing_to_gigavoice_chunks_total"),
    GRPC_INCOMING_FROM_GIGAVOICE_CHUNKS_TOTAL("grpc_incoming_from_gigavoice_chunks_total"),
    GRPC_RESPONSE_TOTAL_TOKENS("grpc_response_tokens_total"),
    GRPC_CONNECTIONS_TOTAL("grpc_connections_total"),
    GRPC_CONNECTIONS_ACTIVE("grpc_connections_active"),
    GRPC_CONNECTIONS_DURATION_SECONDS("grpc_connections_duration_seconds"),
    GRPC_CONNECTIONS_TTFB_SECONDS("grpc_connections_ttfb_seconds"),
    GRPC_ASSISTANT_RESPONSE_SILENCE_TIME_SECONDS("grpc_assistant_response_silence_time_seconds")
}
