package ru.sbrf.ufs.dab2c.core.executor.shared.cbul

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulData
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulOperation
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.CbulConstraintViolationException
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.KeyRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.InMemoryCbulDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.mapper.CbulObjectMapperProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.StaticLocalTimeProvider
import java.util.UUID

class InMemoryCbulDaoServiceTest {

    private lateinit var localTimeProvider: LocalTimeProvider
    private lateinit var objectMapper: ObjectMapper
    private lateinit var daoService: InMemoryCbulDaoService

    @BeforeEach
    fun setUp() {
        localTimeProvider = StaticLocalTimeProvider(1)
        objectMapper = CbulObjectMapperProvider().getMapper()
        daoService = InMemoryCbulDaoService(localTimeProvider, objectMapper)
    }

    @Test
    fun `should save new record and generate unique row id`() {
        val key = KeyRecord("key1", "alias1", null, 0)

        val savedKey = daoService.saveOrUpdate(key, "data1")

        assertFalse(savedKey.uniqueRowId.isNullOrBlank())
        assertEquals(1L, daoService.getDataVersion("key1"))
    }

    @Test
    fun `should save new record with too long alias`() {
        val key = KeyRecord("key1", "a".repeat(31), null, 0)

        assertThrows<CbulConstraintViolationException> {
            daoService.saveOrUpdate(key, "data1")
        }
    }

    @Test
    fun `should read data by key and unique row id`() {
        val key = KeyRecord("key2", "alias2", UUID.randomUUID().toString(), 0)
        val savedKey = daoService.saveOrUpdate(key, "data2")

        val records = daoService.readData(savedKey).toList()

        assertEquals(1, records.size)
        assertEquals("data2", records[0].data)
    }

    @Test
    fun `should remove record by key and mark as deleted`() {
        val key = KeyRecord("key3", "alias3", UUID.randomUUID().toString(), 0)
        daoService.saveOrUpdate(key, "data3")

        daoService.remove(key)

        val records = daoService.readData(key).toList()
        assertEquals(1, records.size)
        assertTrue(records[0].deleted)
    }

    @Test
    fun `should batch change with write and remove operations`() {
        val key1 = KeyRecord("key4", "alias4", UUID.randomUUID().toString(), 0)
        val key2 = KeyRecord("key5", "alias5", UUID.randomUUID().toString(), 0)

        val batchData = listOf(
            BatchCbulData(key1, "data4", BatchCbulOperation.WRITE),
            BatchCbulData(key2, "data5", BatchCbulOperation.WRITE)
        )

        daoService.batchChange(batchData)

        val records1 = daoService.readData(key1).toList()
        assertEquals(1, records1.size)
        assertEquals("data4", records1[0].data)

        val records2 = daoService.readData(key2).toList()
        assertEquals(1, records2.size)
        assertEquals("data5", records2[0].data)

        val newBatchData = listOf(
            BatchCbulData(key1, "data6", BatchCbulOperation.WRITE),
            BatchCbulData(key = key2, operation = BatchCbulOperation.REMOVE)
        )

        daoService.batchChange(newBatchData)

        val recordUpdated = daoService.readData(key1).toList()
        assertEquals(1, recordUpdated.size)
        assertEquals("data6", recordUpdated[0].data)

        val recordDeleted = daoService.readData(key2).toList()
        assertEquals(1, recordDeleted.size)
        assertEquals("data5", recordDeleted[0].data)
    }

    @Test
    fun `should reset storage and clear all data`() {
        val key = KeyRecord("key6", "alias6", UUID.randomUUID().toString(), 0)
        daoService.saveOrUpdate(key, "data6")

        daoService.resetStorage()

        val records = daoService.readData(key).toList()
        assertEquals(0, records.size)
        assertEquals(0L, daoService.getDataVersion("key6"))
    }

    @Test
    fun `should read data by aliases`() {
        val alias1 = KeyRecord("key7", "alias7", UUID.randomUUID().toString(), 0)
        val alias2 = KeyRecord("key7", "alias8", UUID.randomUUID().toString(), 0)
        daoService.saveOrUpdate(alias1, "data7")
        daoService.saveOrUpdate(alias2, "data8")

        val records = daoService.readDataByAliases("key7", listOf("alias7", "alias8")).toList()

        assertEquals(2, records.size)
        assertEquals("data7", records[0].data)
        assertEquals("data8", records[1].data)
    }

    @Test
    fun `should return correct size of storage`() {
        val key1 = KeyRecord("key8", "alias9", UUID.randomUUID().toString(), 0)
        val key2 = KeyRecord("key8", "alias10", UUID.randomUUID().toString(), 0)
        daoService.saveOrUpdate(key1, "data9")
        daoService.saveOrUpdate(key2, "data10")

        val storage = daoService.getStorageAsString()

        assertEquals(1, daoService.size())
        assertEquals(2, daoService.sizeByAlias("key8"))
        assertEquals(1, daoService.sizeByKeyAndAlias("key8", "alias9"))
    }

    @Test
    fun `should return correct storage data after set it by json`() {

        val key1 = KeyRecord("key1", "alias1", "12345", 0)
        daoService.saveOrUpdate(key1, "data9")

        val storageJson = """
            {"key1":{"alias1":{"12345":{"key":{"key":"key1","alias":"alias1","uniqueRowId":"12345","cbulVersion":1},"data":"data9","creationDate":"1970-01-01T03:00:00.001","deleted":false}}}}
        """.trimIndent()

        daoService.setStorage(storageJson)

        assertEquals(storageJson, daoService.getStorageAsString())
    }

    @Test
    fun `should return correct storage version after set it by json`() {

        val key1 = KeyRecord("key1", "alias1", "12345", 0)
        daoService.saveOrUpdate(key1, "data9")

        val versionStorageJson = """
            {"key1":3}
        """.trimIndent()

        daoService.setVersionStorage(versionStorageJson)

        assertEquals(3, daoService.getDataVersion("key1"))
    }
}
