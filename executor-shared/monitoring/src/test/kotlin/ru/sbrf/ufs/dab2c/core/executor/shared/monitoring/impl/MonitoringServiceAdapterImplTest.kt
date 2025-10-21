@file:Suppress("DEPRECATION")

package ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.impl

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricOrigin
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricPath
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricType
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.impl.MonitoringServiceAdapterImpl
import ru.sbrf.ufs.platform.monitoring.CoreMonitoringService
import ru.sbrf.ufs.platform.monitoring.attributes.MetricAttributes

class MonitoringServiceAdapterImplTest {

    private val monitoringService = mockk<CoreMonitoringService>()

    @BeforeEach
    fun beforeEach() {
        clearMocks(monitoringService)
        every { monitoringService.reportEvent(any(), any(), any(), any<MetricAttributes>()) }.returns(Unit)
    }

    @Test
    fun shouldCallMonitoringServiceWithCorrectParameters() {

        val monitoringServiceAdapter = MonitoringServiceAdapterImpl(monitoringService)

        val metricPath = MetricPath(
            origin = MetricOrigin.SYSTEM,
            service = "SERVICE",
            caller = "CALLER",
            action = "ACTION",
            metricType = MetricType.SUCCESS
        )

        monitoringServiceAdapter.reportMetric(metricPath, 1.0)

        verify {
            monitoringService.reportEvent(
                "system-service-caller-action-success",
                1.0,
                "SYSTEM/SERVICE/CALLER/ACTION/SUCCESS",
                any<MetricAttributes>()
            )
        }
    }

    @Test
    fun shouldCallMonitoringServiceWithCorrectParametersAndExcludeNullParts() {

        val monitoringServiceAdapter = MonitoringServiceAdapterImpl(monitoringService)

        val metricPath = MetricPath(
            origin = MetricOrigin.SYSTEM
        )

        monitoringServiceAdapter.reportMetric(metricPath, 1.0)

        verify {
            monitoringService.reportEvent("system", 1.0, "SYSTEM", any<MetricAttributes>())
        }
    }
}
