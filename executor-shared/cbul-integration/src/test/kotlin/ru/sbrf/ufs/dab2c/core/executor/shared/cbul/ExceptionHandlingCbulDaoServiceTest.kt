package ru.sbrf.ufs.dab2c.core.executor.shared.cbul

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulData
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulOperation
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.CbulBulkReadDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.KeyRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.ValueRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.exceptions.CbulException
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.ExceptionHandlingCbulDaoService

class ExceptionHandlingCbulDaoServiceTest {

    private val delegate: CbulBulkReadDaoService = mockk(relaxed = true)
    private val service = ExceptionHandlingCbulDaoService(delegate)

    private val key = KeyRecord("K", "A", "R")
    private val aliases = listOf("A", "B")
    private val data = "data1"
    private val batchData = listOf(
        BatchCbulData(
            KeyRecord("K", "A", "R"),
            "data1",
            BatchCbulOperation.WRITE
        )
    )
    private val expectedSequence = sequenceOf(
        ValueRecord(
            key,
            "data",
            deleted = false
        )
    )

    @Test
    fun `should call delegate saveOrUpdate successfully`() {
        every { delegate.saveOrUpdate(key, data) } returns key

        service.saveOrUpdate(key, data)

        verify(exactly = 1) { delegate.saveOrUpdate(key, data) }
    }

    @Test
    fun `should wrap exception in CbulException when saveOrUpdate fails`() {
        val exception = RuntimeException("DAO error")

        every { delegate.saveOrUpdate(key, data) } throws exception

        assertThrows<CbulException> {
            service.saveOrUpdate(key, data)
        }
    }

    @Test
    fun `should call delegate batchChange successfully`() {
        every { delegate.batchChange(batchData) } returns Unit

        service.batchChange(batchData)

        verify(exactly = 1) { delegate.batchChange(batchData) }
    }

    @Test
    fun `should wrap exception in CbulException when batchChange fails`() {
        val exception = RuntimeException("DAO error")
        every { delegate.batchChange(batchData) } throws exception

        assertThrows<CbulException> {
            service.batchChange(batchData)
        }
    }

    @Test
    fun `should call delegate remove successfully`() {
        every { delegate.remove(key) } returns Unit

        service.remove(key)

        verify(exactly = 1) { delegate.remove(key) }
    }

    @Test
    fun `should wrap exception in CbulException when remove fails`() {
        val exception = RuntimeException("DAO error")
        every { delegate.remove(key) } throws exception

        assertThrows<CbulException> {
            service.remove(key)
        }
    }

    @Test
    fun `should call delegate readData successfully`() {

        every { delegate.readData(key) } returns expectedSequence

        val result = service.readData(key)

        assertEquals(expectedSequence, result)
        verify(exactly = 1) { delegate.readData(key) }
    }

    @Test
    fun `should wrap exception in CbulException when readData fails`() {
        val exception = RuntimeException("DAO error")

        every { delegate.readData(key) } throws exception

        assertThrows<CbulException> {
            service.readData(key).toList()
        }
    }

    @Test
    fun `should call delegate generateUniqueRowId successfully`() {
        every { delegate.generateUniqueRowId() } returns "id123"

        val result = service.generateUniqueRowId()

        assertEquals("id123", result)
        verify(exactly = 1) { delegate.generateUniqueRowId() }
    }

    @Test
    fun `should wrap exception in CbulException when generateUniqueRowId fails`() {
        val exception = RuntimeException("DAO error")

        every { delegate.generateUniqueRowId() } throws exception

        assertThrows<CbulException> {
            service.generateUniqueRowId()
        }
    }

    @Test
    fun `should call delegate getDataVersion successfully`() {
        every { delegate.getDataVersion("key1") } returns 123L

        val result = service.getDataVersion("key1")

        assertEquals(123L, result)
        verify(exactly = 1) { delegate.getDataVersion("key1") }
    }

    @Test
    fun `should wrap exception in CbulException when getDataVersion fails`() {
        val exception = RuntimeException("DAO error")

        every { delegate.getDataVersion("key1") } throws exception

        assertThrows<CbulException> {
            service.getDataVersion("key1")
        }
    }

    @Test
    fun `should call delegate readDataByAliases successfully`() {

        every { delegate.readDataByAliases("K", aliases) } returns expectedSequence

        val result = service.readDataByAliases("K", aliases)

        assertEquals(expectedSequence, result)
        verify(exactly = 1) { delegate.readDataByAliases("K", aliases) }
    }

    @Test
    fun `should wrap exception in CbulException when readDataByAliases fails`() {
        val exception = RuntimeException("DAO error")

        every { delegate.readDataByAliases("K", aliases) } throws exception

        assertThrows<CbulException> {
            service.readDataByAliases("K", aliases).toList()
        }
    }
}
