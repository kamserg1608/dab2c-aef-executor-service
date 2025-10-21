package ru.sbrf.ufs.dab2c.core.executor.shared.cbul

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulData
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulOperation
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.CbulBulkReadDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.KeyRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.ValueRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.MonitoredCbulDaoServiceImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.MonitoredCbulDaoServiceImpl.Companion.BATCH_UPDATE_SIZE
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.aspect.impl.CbulMonitoringAspect.Companion.CBUL_SERVICE_NAME
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricOrigin
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricPath
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricType
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.MonitoringServiceAdapter
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class MonitoredCbulDaoServiceImplTest {

    @MockK(relaxed = true)
    private lateinit var delegate: CbulBulkReadDaoService

    @MockK(relaxed = true)
    private lateinit var monitoringService: MonitoringServiceAdapter

    @InjectMockKs
    private lateinit var service: MonitoredCbulDaoServiceImpl

    @Test
    fun `readDataByAliases should call delegate and return expected sequence of ValueRecord`() {
        // Arrange
        val key = "testKey"
        val aliases = listOf("alias1", "alias2")
        val valueRecordKey = KeyRecord("key1", "domain1", "row1", 1L)
        val valueRecord = ValueRecord(
            key = valueRecordKey,
            data = "data1",
            creationDate = LocalDateTime.now(),
            deleted = false
        )
        val expectedSequence = sequenceOf(valueRecord)

        every { delegate.readDataByAliases(key, aliases) } returns expectedSequence

        // Act
        val result = service.readDataByAliases(key, aliases)

        // Assert
        verify(exactly = 1) { delegate.readDataByAliases(key, aliases) }
        assertThat(result.toList()).isEqualTo(expectedSequence.toList())
    }

    @Test
    fun `saveOrUpdate should call delegate and return expected KeyRecord`() {
        // Arrange
        val key = KeyRecord("key1", "domain1", "row1", 1L)
        val data = "someData"
        val expectedKey = KeyRecord("key1", "domain1", "row1", 2L)

        every { delegate.saveOrUpdate(key, data) } returns expectedKey

        // Act
        val result = service.saveOrUpdate(key, data)

        // Assert
        verify(exactly = 1) { delegate.saveOrUpdate(key, data) }
        assertThat(result).isEqualTo(expectedKey)
    }

    @Test
    fun `batchChange should report metric with correct size and call delegate`() {
        // Arrange
        val batchData = listOf(
            BatchCbulData(key = KeyRecord("key1", "domain1"), operation = BatchCbulOperation.WRITE),
            BatchCbulData(key = KeyRecord("key2", "domain2"), operation = BatchCbulOperation.REMOVE)
        )

        // Act
        service.batchChange(batchData)

        // Assert
        val expectedMetricPath = MetricPath(
            origin = MetricOrigin.SYSTEM,
            action = BATCH_UPDATE_SIZE,
            service = CBUL_SERVICE_NAME,
            metricType = MetricType.VALUE
        )
        verifyOrder {
            monitoringService.reportMetric(eq(expectedMetricPath), eq(2.0))
            delegate.batchChange(batchData)
        }
    }

    @Test
    fun `readData should call delegate and return expected sequence of ValueRecord`() {
        // Arrange
        val key = KeyRecord("key1", "domain1", "row1", 1L)
        val valueRecord = ValueRecord(
            key = key,
            data = "data1",
            creationDate = LocalDateTime.now(),
            deleted = false
        )
        val expectedSequence = sequenceOf(valueRecord)

        every { delegate.readData(key) } returns expectedSequence

        // Act
        val result = service.readData(key)

        // Assert
        verify(exactly = 1) { delegate.readData(key) }
        assertThat(result.toList()).isEqualTo(expectedSequence.toList())
    }

    @Test
    fun `batchChange with empty list should not report 0 metric and call delegate`() {
        // Arrange
        val batchData = emptyList<BatchCbulData>()

        // Act
        service.batchChange(batchData)

        // Assert
        verify(exactly = 0) {
            monitoringService.reportMetric(any(), any())
        }
        verify(exactly = 0) {
            delegate.batchChange(any())
        }
    }
}
