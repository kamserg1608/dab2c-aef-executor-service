package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulData
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulOperation.REMOVE
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulOperation.WRITE
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.CbulDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.KeyRecord

/**
 * Implementation of BatchCbulDaoServiceImpl that batching records.
 */
class BatchCbulDaoServiceImpl(
    private val delegate: CbulDaoService,
    private val collector: LinkedHashMap<String, MutableList<BatchCbulData>> = LinkedHashMap()
) : CbulDaoService by delegate, BatchCbulDaoService {

    override fun saveOrUpdate(key: KeyRecord, data: String): KeyRecord {
        collector.getOrPut(key.key) { mutableListOf() }.add(
            BatchCbulData(
                key = key,
                data = data,
                operation = WRITE
            )
        )
        return key
    }

    override fun remove(key: KeyRecord) {
        collector.getOrPut(key.key) { mutableListOf() }.add(
            BatchCbulData(
                key = key,
                operation = REMOVE
            )
        )
    }

    override fun batchChange(listBatchCbulData: List<BatchCbulData>) {
        listBatchCbulData.forEach {
            when (it.operation) {
                WRITE -> saveOrUpdate(it.key, it.data)
                REMOVE -> remove(it.key)
            }
        }
    }

    override fun flush() {
        collector.forEach { (_, listBatchCbulData) ->
            if (listBatchCbulData.size == 1) {
                val batchCbulData = listBatchCbulData.first()
                when (batchCbulData.operation) {
                    WRITE -> delegate.saveOrUpdate(batchCbulData.key, batchCbulData.data)
                    REMOVE -> delegate.remove(batchCbulData.key)
                }
            } else {
                delegate.batchChange(listBatchCbulData)
            }
        }
        collector.clear()
    }
}
