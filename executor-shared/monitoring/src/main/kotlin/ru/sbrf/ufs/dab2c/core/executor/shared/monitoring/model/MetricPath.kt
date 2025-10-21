package ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model

/**
 * Represents metric path in distributed manner.
 */
data class MetricPath(

    /** Represents the place of appearance of the metric. */
    val origin: MetricOrigin,

    /** The service associated with the metric. */
    val service: String? = null,

    /** The caller associated with the metric. */
    val caller: String? = null,

    /** The action associated with the metric. */
    val action: String? = null,

    /** The status of the metric path. */
    val metricType: MetricType? = null

)
