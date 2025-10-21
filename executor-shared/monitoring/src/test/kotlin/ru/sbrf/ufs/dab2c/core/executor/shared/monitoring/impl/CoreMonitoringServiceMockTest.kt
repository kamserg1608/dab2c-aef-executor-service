package ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.impl

import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.impl.CoreMonitoringServiceMock
import ru.sbrf.ufs.platform.monitoring.attributes.MetricAttributes

class CoreMonitoringServiceMockTest {

    private val monitoringServiceMock = CoreMonitoringServiceMock()

    @Test
    fun reportEventShouldReturnNull() {
        Assertions.assertDoesNotThrow { monitoringServiceMock.reportEvent("", 1.0, "") }
    }

    @Test
    fun reportEventShouldOverloadReturnNull() {
        Assertions.assertDoesNotThrow { monitoringServiceMock.reportEvent("", 1.0, "", "") }
    }

    @Test
    fun reportEventShouldOverloadOverloadReturnNull() {
        Assertions.assertDoesNotThrow {
            monitoringServiceMock.reportEvent(
                "",
                1.0,
                "",
                MetricAttributes.builder().build()
            )
        }
    }

    @Test
    fun updateSslConfigKafkaShouldReturnNull() {
        Assertions.assertNull(monitoringServiceMock.updateSslConfigKafka(mockk()))
    }

    @Test
    fun updateSslConfigKafkaOverloadShouldReturnNull() {
        Assertions.assertNull(monitoringServiceMock.updateSslConfigKafka())
    }

    @Test
    fun updateSslConfigHttpShouldReturnNull() {
        Assertions.assertNull(monitoringServiceMock.updateSslConfigHttp(mockk()))
    }

    @Test
    fun updateSslConfigHttpOverloadShouldReturnNull() {
        Assertions.assertNull(monitoringServiceMock.updateSslConfigHttp())
    }

    @Test
    fun updateSslConfigEndpointShouldReturnNull() {
        Assertions.assertNull(monitoringServiceMock.updateSslConfigEndpoint(null))
    }

    @Test
    fun updateSslConfigEndpointOverloadShouldReturnNull() {
        Assertions.assertNull(monitoringServiceMock.updateSslConfigEndpoint())
    }

    @Test
    fun updateSslConfigPushGatewayShouldReturnNull() {
        Assertions.assertNull(monitoringServiceMock.updateSslConfigPushGateway(null))
    }

    @Test
    fun updateSslConfigPushGatewayOverloadShouldReturnNull() {
        Assertions.assertNull(monitoringServiceMock.updateSslConfigPushGateway())
    }

    @Test
    fun updateSslConfigHttpFiltersOverloadShouldReturnNull() {
        Assertions.assertNull(monitoringServiceMock.updateSslConfigHttpFilters(null))
    }

    @Test
    fun updateSslConfigHttpFiltersShouldReturnNull() {
        Assertions.assertNull(monitoringServiceMock.updateSslConfigHttpFilters())
    }

    @Test
    fun updateSslShouldReturnNull() {
        Assertions.assertNull(monitoringServiceMock.updateSsl())
    }
}
