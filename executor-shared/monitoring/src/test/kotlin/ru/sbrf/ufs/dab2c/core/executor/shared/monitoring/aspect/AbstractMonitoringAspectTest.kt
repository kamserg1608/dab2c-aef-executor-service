package ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.aspect

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.aspectj.lang.ProceedingJoinPoint
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricOrigin
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricPath
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.model.MetricType
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.MonitoringServiceAdapter

class AbstractMonitoringAspectTest {

    val monitoringServiceAdapter = mockk<MonitoringServiceAdapter>()
    private val joinPoint = mockk<ProceedingJoinPoint>()
    private val timeProvider = mockk<LocalTimeProvider>()

    private var monitoringAspect = object : AbstractMonitoringAspect<String>(
        monitoringServiceAdapter,
        timeProvider
    ) {
        override fun advice(joinPoint: ProceedingJoinPoint, config: String): Any? {
            val metricPath = listOf(
                MetricPath(
                    origin = MetricOrigin.INBOUND,
                    service = "SERVICE",
                    caller = "CALLER",
                    action = "ACTION"
                )
            )
            return executionWrapper(joinPoint, metricPath)
        }
    }

    private val baseMetricPath = MetricPath(
        origin = MetricOrigin.INBOUND,
        service = "SERVICE",
        caller = "CALLER",
        action = "ACTION"
    )

    @BeforeEach
    fun setUp() {
        clearMocks(monitoringServiceAdapter, joinPoint, timeProvider)
        every { monitoringServiceAdapter.reportMetric(any(), any()) }.returns(Unit)
    }

    @Test
    fun shouldReportSuccessIfJoinPointSucceed() {
        every { joinPoint.proceed() }.returns(Unit)
        every { timeProvider.currentTimeMillis() }.returnsMany(5L, 10L)

        monitoringAspect.advice(joinPoint, "CONFIG")

        val expectedFirstCall = baseMetricPath.copy(metricType = MetricType.SUCCESS)
        val expectedSecondCall = baseMetricPath.copy(metricType = MetricType.SUCCESS_TIME)
        verify(exactly = 1) { monitoringServiceAdapter.reportMetric(expectedFirstCall, 1.0) }
        verify(exactly = 1) { monitoringServiceAdapter.reportMetric(expectedSecondCall, 5.0) }
    }

    @Test
    fun shouldReportErrorIfJoinPointThrows() {
        every { joinPoint.proceed() }.throws(Exception("ERROR"))
        every { timeProvider.currentTimeMillis() }.returnsMany(5L, 10L)

        try {
            monitoringAspect.advice(joinPoint, "CONFIG")
        } catch (_: Exception) { }

        val expectedFirstCall = baseMetricPath.copy(metricType = MetricType.ERROR)
        val expectedSecondCall = baseMetricPath.copy(metricType = MetricType.ERROR_TIME)

        verify(exactly = 1) { monitoringServiceAdapter.reportMetric(expectedFirstCall, 1.0) }
        verify(exactly = 1) { monitoringServiceAdapter.reportMetric(expectedSecondCall, 5.0) }
    }
}
