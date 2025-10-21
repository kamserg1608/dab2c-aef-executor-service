package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.impl

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.aspectj.lang.ProceedingJoinPoint
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.CircuitBreakerRegistry
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.impl.CircuitBreakerServiceImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider
import java.time.Duration
import java.util.concurrent.TimeUnit

class CircuitBreakerServiceImplTest {

    private val circuitbreakerRegistry: CircuitBreakerRegistry = mockk(relaxed = true)
    private val localTimeProvider: LocalTimeProvider = mockk(relaxed = true)
    private val circuitBreaker: CircuitBreaker = mockk(relaxed = true)
    private val joinPoint: ProceedingJoinPoint = mockk(relaxed = true)

    val circuitBreakerService = CircuitBreakerServiceImpl(
        circuitbreakerRegistry,
        localTimeProvider
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { circuitbreakerRegistry.getCircuitBreaker(SERVICE_NAME) }.returns(circuitBreaker)
        every { circuitBreaker.onSuccess(any(), any()) }.returns(Unit)
        every { circuitBreaker.onError(any(), any(), any()) }.returns(Unit)
        every { localTimeProvider.currentTimeMillis() }.returnsMany(5, 10)
    }

    @Test
    fun `test interruptExecution happy path`() {
        every { circuitBreaker.acquirePermission() }.returns(Unit)
        every { joinPoint.proceed() }.returns(JOIN_POINT_RESPONSE)

        val result = circuitBreakerService.interruptExecution(joinPoint, SERVICE_NAME)

        Assertions.assertEquals(result, JOIN_POINT_RESPONSE)

        verify { joinPoint.proceed() }
        verify { circuitBreaker.acquirePermission() }
        verify { circuitBreaker.onSuccess(5, TimeUnit.MILLISECONDS) }
    }

    @Test
    fun `test interruptExecution permission is not acquired`() {
        every { circuitBreaker.acquirePermission() }.throws(Exception())
        every { joinPoint.proceed() }.returns(JOIN_POINT_RESPONSE)

        Assertions.assertThrows(Exception::class.java) {
            circuitBreakerService.interruptExecution(joinPoint, SERVICE_NAME)
        }

        verify { circuitBreaker.acquirePermission() }
        verify(exactly = 0) { circuitBreaker.onSuccess(any(), any()) }
        verify(exactly = 0) { circuitBreaker.onError(any(), any(), any()) }
        verify(exactly = 0) { joinPoint.proceed() }
    }

    @Test
    fun `test interruptExecution permission acquired by real CircuitBreaker`() {

        val circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(1.0F)
            .automaticTransitionFromOpenToHalfOpenEnabled(false)
            .waitDurationInOpenState(Duration.parse("PT10M"))
            .build()

        val realCircuitBreaker = CircuitBreaker.of(SERVICE_NAME, circuitBreakerConfig)
        realCircuitBreaker.transitionToClosedState()

        every { circuitbreakerRegistry.getCircuitBreaker(SERVICE_NAME) }.returns(realCircuitBreaker)
        every { joinPoint.proceed() }.returns(JOIN_POINT_RESPONSE)

        val result = circuitBreakerService.interruptExecution(joinPoint, SERVICE_NAME)

        Assertions.assertEquals(result, JOIN_POINT_RESPONSE)

        verify { joinPoint.proceed() }
    }

    @Test
    fun `test interruptExecution permission is not acquired by real CircuitBreaker`() {

        val circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(1.0F)
            .automaticTransitionFromOpenToHalfOpenEnabled(false)
            .waitDurationInOpenState(Duration.parse("PT10M"))
            .build()

        val realCircuitBreaker = CircuitBreaker.of(SERVICE_NAME, circuitBreakerConfig)
        realCircuitBreaker.transitionToForcedOpenState()

        every { circuitbreakerRegistry.getCircuitBreaker(SERVICE_NAME) }.returns(realCircuitBreaker)
        every { joinPoint.proceed() }.returns(JOIN_POINT_RESPONSE)

        Assertions.assertThrows(Exception::class.java) {
            circuitBreakerService.interruptExecution(joinPoint, SERVICE_NAME)
        }

        verify(exactly = 0) { joinPoint.proceed() }
    }

    @Test
    fun `test interruptExecution joinPoint proceed fails  `() {
        every { circuitBreaker.acquirePermission() }.returns(Unit)
        every { joinPoint.proceed() }.throws(Exception())

        Assertions.assertThrows(Exception::class.java) {
            circuitBreakerService.interruptExecution(joinPoint, SERVICE_NAME)
        }

        verify { circuitBreaker.acquirePermission() }
        verify(exactly = 1) { joinPoint.proceed() }
        verify(exactly = 1) { circuitBreaker.onError(5, TimeUnit.MILLISECONDS, any()) }
        verify(exactly = 0) { circuitBreaker.onSuccess(any(), any()) }
    }

    companion object {
        private const val SERVICE_NAME = "serviceName"
        private val JOIN_POINT_RESPONSE = ResponseEntity.ok()
    }
}
