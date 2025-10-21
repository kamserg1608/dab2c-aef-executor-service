package ru.sbrf.ufs.dab2c.core.executor.shared.cbul

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.cbul.client.dto.CbulRecord
import ru.sbrf.cbul.client.exceptions.CbulNotFoundDataException
import ru.sbrf.cbul.client.service.BatchApiQueryFactory
import ru.sbrf.cbul.client.service.CbulClientService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulData
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulOperation
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.CbulConstraintViolationException
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.KeyRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.CbulDaoServiceImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CbulDaoServiceImplTest {

    private val batchApiQueryFactory: BatchApiQueryFactory = mockk(relaxed = true)
    private val batchApiQuery: BatchApiQueryFactory.BatchApiQuery = mockk(relaxed = true)
    private val cbulClientService: CbulClientService = mockk(relaxed = true)
    private val localTimeProvider: LocalTimeProvider = mockk(relaxed = true)
    private val cbulDaoService = CbulDaoServiceImpl(
        cbulClientService = cbulClientService,
        localTimeProvider = localTimeProvider
    ) { batchApiQueryFactory }

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { localTimeProvider.now() } returns NOW
        every { batchApiQueryFactory.newQuery(any(), any()) } returns batchApiQuery
        every { batchApiQuery.flushCommands() } returns 0
        every { batchApiQuery.addOrUpdateRow(any(), any(), any(), any()) } returns batchApiQuery
    }

    @Test
    fun `test save or update with correct fields`() {
        val key = KeyRecord("key", "alias")
        val data = "test data"
        val now = LocalDateTime.of(2025, 10, 5, 10, 0)
        val version = 42L
        val generatedRowId = "generated-id"

        every { localTimeProvider.now() } returns now
        every { cbulDaoService.generateUniqueRowId() } returns generatedRowId
        every {
            cbulClientService.addOrUpdateRow(
                key.key,
                key.alias,
                now,
                generatedRowId,
                data
            )
        } returns version

        val result = cbulDaoService.saveOrUpdate(key, data)

        assertEquals(key.key, result.key)
        assertEquals(key.alias, result.alias)
        assertEquals(generatedRowId, result.uniqueRowId)
    }

    @Test
    fun `test batchChange should perform proper chain of calls`() {

        val records = listOf(
            BatchCbulData(
                key = KeyRecord("K", "A-1", "R-1"),
                data = "D-1",
                operation = BatchCbulOperation.WRITE
            ),
            BatchCbulData(
                key = KeyRecord("K", "A-2", "R-2"),
                data = "D-2",
                operation = BatchCbulOperation.WRITE
            ),
            BatchCbulData(
                key = KeyRecord("K", "A-3", "R-3"),
                data = "D-3",
                operation = BatchCbulOperation.REMOVE
            )
        )

        cbulDaoService.batchChange(records)

        verify { batchApiQueryFactory.newQuery("K", 3) }
        verify(exactly = 2) { batchApiQuery.addOrUpdateRow(any(), NOW, any(), any()) }
        verify(exactly = 1) { batchApiQuery.deleteRow(any(), any()) }
        verify { batchApiQuery.flushCommands() }
    }

    @Test
    fun `test batchChange should fails on different keys in batch`() {

        val records = listOf(
            BatchCbulData(
                key = KeyRecord("K", "A-1", "R-1"),
                data = "D-1",
                operation = BatchCbulOperation.WRITE
            ),
            BatchCbulData(
                key = KeyRecord("K", "A-2", "R-2"),
                data = "D-2",
                operation = BatchCbulOperation.WRITE
            ),
            BatchCbulData(
                key = KeyRecord("K1", "A-3", "R-3"),
                data = "D-3",
                operation = BatchCbulOperation.REMOVE
            )
        )

        Assertions.assertThatThrownBy {
            cbulDaoService.batchChange(records)
        }.isInstanceOf(IllegalStateException::class.java)

        verify(exactly = 0) { batchApiQueryFactory.newQuery(any(), any()) }
    }

    @Test
    fun `test batchChange should fails on key constraints violations`() {

        val records = listOf(
            BatchCbulData(
                key = KeyRecord("K".repeat(129), "A-1", "R-1"),
                data = "D-1",
                operation = BatchCbulOperation.WRITE
            )
        )

        Assertions.assertThatThrownBy {
            cbulDaoService.batchChange(records)
        }.isInstanceOf(CbulConstraintViolationException::class.java)

        verify { batchApiQueryFactory.newQuery(any(), any()) }
        verify(exactly = 0) { batchApiQuery.addOrUpdateRow(any(), NOW, any(), any()) }
        verify(exactly = 0) { batchApiQuery.flushCommands() }
    }

    @Test
    fun `should generate uniqueRowId if it is null in key`() {
        val key = KeyRecord("key", "alias")
        val generatedRowId = "generated-id"
        val data = "test data"
        val now = LocalDateTime.of(2025, 10, 5, 10, 0)
        val version = 42L

        every { localTimeProvider.now() } returns now
        every { cbulClientService.generateUniqueRowId() } returns generatedRowId
        every {
            cbulClientService.addOrUpdateRow(
                key.key,
                key.alias,
                now,
                generatedRowId,
                data
            )
        } returns version

        val result = cbulDaoService.saveOrUpdate(key, data)

        assertEquals(generatedRowId, result.uniqueRowId)
    }

    @Test
    fun `should return ValueRecords from Cbul client`() {
        val key = "key"
        val aliases = listOf("alias1", "alias2")
        val records = listOf(
            createCbulRecord("1", "alias1", "data1", LocalDateTime.now()),
            createCbulRecord("2", "alias2", "data2", LocalDateTime.now())
        )

        every { cbulClientService.readDataByAliases(key, aliases.toTypedArray()) } returns records

        val result = cbulDaoService.readDataByAliases(key, aliases).toList()

        assertEquals(2, result.size)
        assertEquals("data1", result[0].data)
        assertEquals("data2", result[1].data)
    }

    @Test
    fun `should read data by alias and uniqueRowId if provided`() {
        val key = KeyRecord("key", "alias", "rowId")
        val record = createCbulRecord("1", "alias1", "data1", LocalDateTime.now())

        every { cbulClientService.readRecord(key.key, key.alias, key.uniqueRowId) } returns record

        val result = cbulDaoService.readData(key).toList()

        assertEquals(1, result.size)
        assertEquals("data1", result[0].data)
    }

    @Test
    fun `should read data by alias if uniqueRowId is null`() {
        val key = KeyRecord("key", "alias")
        val records = listOf(createCbulRecord("1", "alias1", "data1", LocalDateTime.now()))

        every { cbulClientService.readDataByAliases(key.key, arrayOf(key.alias)) } returns records

        val result = cbulDaoService.readData(key).toList()

        assertEquals(1, result.size)
        assertEquals("data1", result[0].data)
    }

    @Test
    fun `should return empty sequence if record not found`() {
        val key = KeyRecord("key", "alias", "rowId")

        every {
            cbulClientService.readRecord(key.key, key.alias, key.uniqueRowId)
        } throws CbulNotFoundDataException("Error")

        val result = cbulDaoService.readData(key).toList()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should delete row by key and alias and uniqueRowId`() {
        val key = KeyRecord("key", "alias", "rowId")

        cbulDaoService.remove(key)

        verify(exactly = 1) { cbulClientService.deleteRow(key.key, key.alias, key.uniqueRowId) }
    }

    private fun createCbulRecord(cbulId: String, alias: String, data: String, creationDate: LocalDateTime): CbulRecord =
        CbulRecord().apply {
            this.cbulId = "1"
            this.alias = alias
            this.data = data.toByteArray()
            this.charset = "UTF-8"
            this.creationDate = creationDate
        }

    companion object {
        private val NOW = LocalDateTime.of(LocalDate.EPOCH, LocalTime.MIDNIGHT)
    }
}
