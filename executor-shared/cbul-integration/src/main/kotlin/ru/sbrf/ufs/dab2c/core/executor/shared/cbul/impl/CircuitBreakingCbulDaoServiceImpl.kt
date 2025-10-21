package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulData
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.CbulBulkReadDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.KeyRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.ValueRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.annotation.ServiceCircuitBreaker

/**
 * CircuitBreaking implementation of [CbulBulkReadDaoService] based on delegate pattern.
 */
open class CircuitBreakingCbulDaoServiceImpl(
    private val delegate: CbulBulkReadDaoService,
) : CbulBulkReadDaoService by delegate {

    @ServiceCircuitBreaker(SERVICE_NAME)
    override fun readDataByAliases(key: String, aliases: Iterable<String>): Sequence<ValueRecord> =
        this.delegate.readDataByAliases(key, aliases)

    @ServiceCircuitBreaker(SERVICE_NAME)
    override fun saveOrUpdate(key: KeyRecord, data: String): KeyRecord = delegate.saveOrUpdate(key, data)

    @ServiceCircuitBreaker(SERVICE_NAME)
    override fun batchChange(listBatchCbulData: List<BatchCbulData>) =
        delegate.batchChange(listBatchCbulData)

    @ServiceCircuitBreaker(SERVICE_NAME)
    override fun readData(key: KeyRecord): Sequence<ValueRecord> = delegate.readData(key)

    internal companion object {
        internal const val SERVICE_NAME = "cbul"
    }
}
