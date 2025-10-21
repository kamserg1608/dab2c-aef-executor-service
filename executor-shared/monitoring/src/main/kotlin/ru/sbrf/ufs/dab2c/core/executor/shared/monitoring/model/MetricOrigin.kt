package ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model

/** Represents the origin of a metric. */
enum class MetricOrigin {

    /** Indicates that the metric is related to an inbound operation. */
    INBOUND,

    /** Indicates that the metric is related to an outbound operation. */
    OUTBOUND,

    /** Indicates that the metric is related to system operation. */
    SYSTEM
}
