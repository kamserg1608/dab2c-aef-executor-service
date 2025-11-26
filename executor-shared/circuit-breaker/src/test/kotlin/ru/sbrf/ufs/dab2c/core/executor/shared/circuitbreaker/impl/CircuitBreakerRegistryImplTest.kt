package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.impl

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.CircuitBreakerParameters
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.model.PmsCircuitBreakerConfig
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.impl.CircuitBreakerRegistryImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.impl.NoOpCircuitBreaker
import java.time.Duration
import java.util.Collections.emptySet

// @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
@Suppress("FunctionMaxLength")
class CircuitBreakerRegistryImplTest {

    private val circuitBreakerParameters: CircuitBreakerParameters = mockk(relaxed = true)

    private var circuitbreakerRegistry = CircuitBreakerRegistryImpl(circuitBreakerParameters)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        circuitbreakerRegistry = CircuitBreakerRegistryImpl(circuitBreakerParameters)
    }

    @Test
    fun `test getCircuitBreaker should return NoOpCircuitBreaker when CB disabled`() {
        every { circuitBreakerParameters.getParameters(SERVICE_NAME) }
            .returns(initialCircuitBreakerConfig.copy(isEnabled = false))
        every { circuitBreakerParameters.subscribe(SERVICE_NAME, any()) }.returns(Unit)

        val circuitBreaker = circuitbreakerRegistry.getCircuitBreaker(SERVICE_NAME)

        Assertions.assertInstanceOf(NoOpCircuitBreaker::class.java, circuitBreaker)
        verify { circuitBreakerParameters.getParameters(SERVICE_NAME) }
        verify { circuitBreakerParameters.subscribe(SERVICE_NAME, any()) }
    }

    @Test
    fun `test getCircuitBreaker should return circuitBreaker and subscribe it to updates`() {
        every { circuitBreakerParameters.getParameters(SERVICE_NAME) }.returns(initialCircuitBreakerConfig)
        every { circuitBreakerParameters.subscribe(SERVICE_NAME, any()) }.returns(Unit)

        val circuitBreaker = circuitbreakerRegistry.getCircuitBreaker(SERVICE_NAME)

        Assertions.assertNotNull(circuitBreaker)
        verify { circuitBreakerParameters.getParameters(SERVICE_NAME) }
        verify { circuitBreakerParameters.subscribe(SERVICE_NAME, any()) }
    }

    @Test
    fun `test getCircuitBreaker should return same circuitBreaker if called twice`() {
        every { circuitBreakerParameters.getParameters(SERVICE_NAME) }.returns(initialCircuitBreakerConfig)
        every { circuitBreakerParameters.subscribe(SERVICE_NAME, any()) }.returns(Unit)

        val firstCallCircuitBreaker = circuitbreakerRegistry.getCircuitBreaker(SERVICE_NAME)
        val secondCallCircuitBreaker = circuitbreakerRegistry.getCircuitBreaker(SERVICE_NAME)

        Assertions.assertSame(firstCallCircuitBreaker, secondCallCircuitBreaker)
        verify(exactly = 1) { circuitBreakerParameters.getParameters(SERVICE_NAME) }
        verify(exactly = 1) { circuitBreakerParameters.subscribe(SERVICE_NAME, any()) }
    }

    @Test
    fun `test getCircuitBreaker should return other circuitBreaker after mutation`() {
        val mutationCallback = mutableListOf<() -> Unit>()
        every { circuitBreakerParameters.getParameters(SERVICE_NAME) }.returns(initialCircuitBreakerConfig)
        every { circuitBreakerParameters.subscribe(SERVICE_NAME, capture(mutationCallback)) }.returns(Unit)

        val firstCallCircuitBreaker = circuitbreakerRegistry.getCircuitBreaker(SERVICE_NAME)

        mutationCallback.forEach { it.invoke() }

        val secondCallCircuitBreaker = circuitbreakerRegistry.getCircuitBreaker(SERVICE_NAME)

        Assertions.assertNotSame(firstCallCircuitBreaker, secondCallCircuitBreaker)
        verify(exactly = 2) { circuitBreakerParameters.getParameters(SERVICE_NAME) }
        verify(exactly = 1) { circuitBreakerParameters.subscribe(SERVICE_NAME, any()) }
    }

    companion object {
        private const val SERVICE_NAME = "service-name"
        val initialCircuitBreakerConfig = PmsCircuitBreakerConfig(
            serviceName = SERVICE_NAME,
            isEnabled = true,
            failureRateThreshold = 1.0f,
            recordedExceptions = emptySet(),
            ignoredExceptions = emptySet(),
            permittedNumberOfCallsInHalfOpenState = 10,
            slidingWindow = 10,
            waitInOpenStateDuration = Duration.ofSeconds(10),
        )
    }
}
