package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl

import org.slf4j.LoggerFactory
import ru.sbrf.cbul.starter.CbulConfigService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.AdditionalCbulParameters
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulData
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.CbulBulkReadDaoService

/**
 * [CbulBulkReadDaoService] splitting batch change into separate batches
 * of length [cbulParameters.getCbulBatchSize] at most.
 */
class BatchSizeAwareCbulBulkReadDaoService(
    private val delegate: CbulBulkReadDaoService,
    private val cbulParameters: AdditionalCbulParameters
) : CbulBulkReadDaoService by delegate {

    private val logger = LoggerFactory.getLogger(BatchSizeAwareCbulBulkReadDaoService::class.java)

    override fun batchChange(listBatchCbulData: List<BatchCbulData>) {
        if (listBatchCbulData.isEmpty()) {
            return
        }

        val batches = listBatchCbulData.chunked(cbulParameters.getCbulBatchSize())
        if (batches.size > 1) {
            logger.warn("Consider increasing [${CbulConfigService.CBUL_BATCH_SIZE}]. Batches count: ${batches.size}")
        }
        batches.forEach { batch ->
            delegate.batchChange(batch)
        }
    }
}
