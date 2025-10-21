package ru.sbrf.ufs.dab2c.core.executor.shared.cbul

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.AdditionalCbulParameters
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulData
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulOperation
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.CbulBulkReadDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.KeyRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.BatchSizeAwareCbulBulkReadDaoService

const val TEST_INIT_BATCH_SIZE = 10
const val TEST_ACTUAL_BATCH_SIZE = 3
const val TEST_EXPECTED_BATCHES = 4

@ExtendWith(MockKExtension::class)
class BatchSizeAwareCbulBulkReadDaoServiceTest {

    @InjectMockKs
    private lateinit var service: BatchSizeAwareCbulBulkReadDaoService

    @MockK(relaxed = true)
    private lateinit var cbulDaoService: CbulBulkReadDaoService

    @MockK
    private lateinit var additionalCbulParameters: AdditionalCbulParameters

    @Test
    fun splitsBatches() {

        every {
            additionalCbulParameters.getCbulBatchSize()
        } returns TEST_ACTUAL_BATCH_SIZE

        val initialBatch = (0..TEST_INIT_BATCH_SIZE)
            .filter { it % 2 == 0 }
            .flatMap { index ->
                sequenceOf(
                    BatchCbulData(
                        key = KeyRecord(
                            key = "test-key$index",
                            alias = "test-alias$index",
                            uniqueRowId = "test-unique-row-id$index"
                        ),
                        operation = BatchCbulOperation.WRITE
                    ),
                    BatchCbulData(
                        key = KeyRecord(
                            key = "test-key$index",
                            alias = "test-alias${index + 1}",
                            uniqueRowId = "test-unique-row-id${index + 1}"
                        ),
                        operation = BatchCbulOperation.REMOVE
                    )
                )
            }

        service.batchChange(initialBatch)

        verify(exactly = TEST_EXPECTED_BATCHES) {
            cbulDaoService.batchChange(any())
        }
    }

    @Test
    fun emptyBatch() {
        service.batchChange(emptyList())
        verify(exactly = 0) {
            cbulDaoService.batchChange(any())
        }
    }
}
