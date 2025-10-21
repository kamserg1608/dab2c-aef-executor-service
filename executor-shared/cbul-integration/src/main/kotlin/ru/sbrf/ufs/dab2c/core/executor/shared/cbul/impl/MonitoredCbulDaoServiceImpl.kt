package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulData
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.CbulBulkReadDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.KeyRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.ValueRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.annotation.CbulMonitored
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.aspect.impl.CbulMonitoringAspect
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricOrigin
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricPath
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricType
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.MonitoringServiceAdapter

/**
 * Monitoring implementation of [CbulBulkReadDaoService] based on delegate pattern.
 */
open class MonitoredCbulDaoServiceImpl(
    private val delegate: CbulBulkReadDaoService,
    private val monitoringService: MonitoringServiceAdapter
) : CbulBulkReadDaoService by delegate {

    @CbulMonitored(ACTION_GET)
    override fun readDataByAliases(key: String, aliases: Iterable<String>): Sequence<ValueRecord> =
        this.delegate.readDataByAliases(key, aliases)

    @CbulMonitored(ACTION_UPDATE)
    override fun saveOrUpdate(key: KeyRecord, data: String): KeyRecord = delegate.saveOrUpdate(key, data)

    @CbulMonitored(ACTION_BATCH_UPDATE)
    override fun batchChange(listBatchCbulData: List<BatchCbulData>) {
        if (listBatchCbulData.isEmpty()) {
            return
        }

        monitoringService.reportMetric(
            MetricPath(
                origin = MetricOrigin.SYSTEM,
                action = BATCH_UPDATE_SIZE,
                service = CbulMonitoringAspect.CBUL_SERVICE_NAME,
                metricType = MetricType.VALUE
            ),
            listBatchCbulData.size.toDouble()
        )
        delegate.batchChange(listBatchCbulData)
    }

    @CbulMonitored(ACTION_GET)
    override fun readData(key: KeyRecord): Sequence<ValueRecord> = delegate.readData(key)

    internal companion object {
        internal const val ACTION_GET = "GET"
        internal const val ACTION_UPDATE = "UPDATE"
        internal const val ACTION_BATCH_UPDATE = "BATCH_UPDATE"
        internal const val BATCH_UPDATE_SIZE = "BATCH_UPDATE_SIZE"
    }
}
