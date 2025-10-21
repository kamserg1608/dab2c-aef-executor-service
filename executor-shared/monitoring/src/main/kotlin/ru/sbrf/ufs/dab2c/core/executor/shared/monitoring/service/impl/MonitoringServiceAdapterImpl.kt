@file:Suppress("DEPRECATION")

package ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricPath
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.MonitoringServiceAdapter
import ru.sbrf.ufs.platform.monitoring.CoreMonitoringService
import ru.sbrf.ufs.platform.monitoring.attributes.MetricAttributes

/**
 * Base implementation of [MonitoringServiceAdapter].
 */
class MonitoringServiceAdapterImpl(
    private val monitoringService: CoreMonitoringService
) : MonitoringServiceAdapter {

    override fun reportMetric(
        metricPath: MetricPath,
        value: Double
    ) {
        monitoringService.reportEvent(
            buildName(metricPath),
            value,
            buildPath(metricPath),
            MetricAttributes.builder().build()
        )
    }

    private fun buildPath(metricPath: MetricPath) = sequenceOf(
        metricPath.origin.name,
        metricPath.service,
        metricPath.caller,
        metricPath.action,
        metricPath.metricType?.name
    )
        .filterNotNull()
        .joinToString("/")

    private fun buildName(metricPath: MetricPath) = sequenceOf(
        metricPath.origin.name,
        metricPath.service,
        metricPath.caller,
        metricPath.action,
        metricPath.metricType?.name
    )
        .filterNotNull()
        .joinToString("-")
        .lowercase()
}
