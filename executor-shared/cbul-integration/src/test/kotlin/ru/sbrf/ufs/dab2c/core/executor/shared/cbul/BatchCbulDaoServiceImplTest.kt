package ru.sbrf.ufs.dab2c.core.executor.shared.cbul

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulData
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulOperation
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.CbulDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.KeyRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.BatchCbulDaoServiceImpl

class BatchCbulDaoServiceImplTest {

    private val delegate: CbulDaoService = mockk(relaxed = true)
    private val batchCbulDaoService = BatchCbulDaoServiceImpl(delegate)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `test saveOrUpdate should add entry to collector without flush`() {
        val keyRecord = KeyRecord("K", "A", "R")
        val data = "D"

        batchCbulDaoService.saveOrUpdate(keyRecord, data)

        verify(exactly = 0) { delegate.batchChange(any()) }
    }

    @Test
    fun `test single save operation should stay single`() {
        val keyRecord = KeyRecord("K", "A", "R")
        val data = "D"

        batchCbulDaoService.saveOrUpdate(keyRecord, data)
        verify(exactly = 0) { delegate.saveOrUpdate(keyRecord, data) }

        batchCbulDaoService.flush()
        verify(exactly = 1) { delegate.saveOrUpdate(keyRecord, data) }
    }

    @Test
    fun `test single remove operation should stay single`() {
        val keyRecord = KeyRecord("K", "A", "R")

        batchCbulDaoService.remove(keyRecord)
        verify(exactly = 0) { delegate.remove(keyRecord) }

        batchCbulDaoService.flush()
        verify(exactly = 1) { delegate.remove(keyRecord) }
    }

    @Test
    fun `test saveOrUpdate with multiple entries should not call delegate`() {
        val entries = listOf(
            BatchCbulData(
                key = KeyRecord("K", "A", "R"),
                data = "D",
                operation = BatchCbulOperation.WRITE
            ),
            BatchCbulData(
                key = KeyRecord("K", "A2", "R2"),
                data = "D2",
                operation = BatchCbulOperation.WRITE
            ),
            BatchCbulData(
                key = KeyRecord("K", "A3", "R3"),
                data = "D3",
                operation = BatchCbulOperation.WRITE
            )
        )

        batchCbulDaoService.batchChange(entries)

        verify(exactly = 0) { delegate.batchChange(any()) }
    }

    @Test
    fun `test flush should apply all writing entries in order and clear the collector`() {
        val keyRecord = KeyRecord("K", "A", "R")
        val keyRecord2 = KeyRecord("K", "A2", "R")
        val data = "D"

        val entries = listOf(
            BatchCbulData(
                key = keyRecord,
                data = data,
                operation = BatchCbulOperation.WRITE
            ),
            BatchCbulData(
                key = keyRecord2,
                data = data,
                operation = BatchCbulOperation.WRITE
            )
        )

        batchCbulDaoService.saveOrUpdate(keyRecord, data)
        batchCbulDaoService.saveOrUpdate(keyRecord2, data)
        batchCbulDaoService.flush()

        verify(exactly = 1) { delegate.batchChange(entries) }
    }

    @Test
    fun `test flush should apply all removing entries in order and clear the collector`() {
        val keyRecord = KeyRecord("K", "A", "R")
        val keyRecord2 = KeyRecord("K", "A2", "R")

        val entries = listOf(
            BatchCbulData(
                key = keyRecord,
                operation = BatchCbulOperation.REMOVE
            ),
            BatchCbulData(
                key = keyRecord2,
                operation = BatchCbulOperation.REMOVE
            )
        )

        batchCbulDaoService.remove(keyRecord)
        batchCbulDaoService.remove(keyRecord2)
        batchCbulDaoService.flush()

        verify(exactly = 1) { delegate.batchChange(entries) }
    }

    @Test
    fun `test saveOrUpdate with multiple keys should not flush until flush is called`() {
        val entries = listOf(
            BatchCbulData(
                key = KeyRecord("K1", "A1", "R1"),
                data = "D1",
                operation = BatchCbulOperation.WRITE
            ),
            BatchCbulData(
                key = KeyRecord("K2", "A2", "R2"),
                data = "D2",
                operation = BatchCbulOperation.WRITE
            )
        )

        batchCbulDaoService.batchChange(entries)

        verify(exactly = 0) { delegate.batchChange(any()) }
    }

    @Test
    fun `test flush after failure should not re-apply failed operations`() {
        every { delegate.batchChange(any()) } throws RuntimeException("Simulated failure")

        val keyRecord = KeyRecord("K", "A", "R")
        val secondRecord = KeyRecord("K", "A2", "R2")
        val thirdRecord = KeyRecord("K2", "A2", "R2")
        val data = "D"

        val expectedBatch = listOf(
            BatchCbulData(
                key = keyRecord,
                data = data,
                operation = BatchCbulOperation.WRITE
            ),
            BatchCbulData(
                key = secondRecord,
                data = data,
                operation = BatchCbulOperation.WRITE
            )
        )

        batchCbulDaoService.saveOrUpdate(keyRecord, data)
        batchCbulDaoService.saveOrUpdate(secondRecord, data)
        batchCbulDaoService.saveOrUpdate(thirdRecord, data)

        Assertions.assertThatThrownBy {
            batchCbulDaoService.flush()
        }.isInstanceOf(RuntimeException::class.java)

        verify(exactly = 1) {
            delegate.batchChange(any())
        }
        verify(exactly = 1) {
            delegate.batchChange(expectedBatch)
        }
    }

    @Test
    fun `test flush with write and delete operations should apply them`() {
        val keyRecord1 = KeyRecord("K1", "A1", "R1")
        val keyRecord2 = KeyRecord("K2", "A2", "R2")
        val keyRecord3 = KeyRecord("K1", "A3", "R3")
        val keyRecord4 = KeyRecord("K2", "A4", "R4")
        val data = "D"

        batchCbulDaoService.saveOrUpdate(keyRecord1, data)
        batchCbulDaoService.saveOrUpdate(keyRecord2, data)
        batchCbulDaoService.remove(keyRecord3)
        batchCbulDaoService.remove(keyRecord4)

        batchCbulDaoService.flush()

        verify(exactly = 1) {
            delegate.batchChange(
                listOf(
                    BatchCbulData(
                        key = keyRecord1,
                        data = "D",
                        operation = BatchCbulOperation.WRITE
                    ),
                    BatchCbulData(
                        key = keyRecord3,
                        operation = BatchCbulOperation.REMOVE
                    )
                )
            )
        }

        verify(exactly = 1) {
            delegate.batchChange(
                listOf(
                    BatchCbulData(
                        key = keyRecord2,
                        data = "D",
                        operation = BatchCbulOperation.WRITE
                    ),
                    BatchCbulData(
                        key = keyRecord4,
                        operation = BatchCbulOperation.REMOVE
                    )
                )
            )
        }
    }
}
