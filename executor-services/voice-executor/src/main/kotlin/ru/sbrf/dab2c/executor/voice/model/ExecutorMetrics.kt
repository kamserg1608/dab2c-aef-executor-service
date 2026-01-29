package ru.sbrf.dab2c.executor.voice.model

import ru.sbrf.dab2c.executor.library.monitoring.service.api.Metric

/**
 * Metrics for voice executor.
 */
enum class ExecutorMetrics(
    override var metricName: String
) : Metric {
    GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL("grpc_incoming_from_initiator_chunks_total"),
    GRPC_OUTGOING_FROM_INITIATOR_CHUNKS_TOTAL("grpc_outgoing_to_initiator_chunks_total")
}
