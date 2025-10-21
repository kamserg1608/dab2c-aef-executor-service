package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.aspect

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.annotation.ServiceCircuitBreaker
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.CircuitBreakerService

/**
 * Aspect integrating circuit breaker around [ServiceCircuitBreaker]-marked point cut.
 */
@Aspect
class CircuitBreakerAspect(
    private val circuitbreakerService: CircuitBreakerService
) {

    /**
     * Performs circuit breaker protected sync method join point execution.
     */
    @Around("@annotation(config)")
    fun advice(joinPoint: ProceedingJoinPoint, config: ServiceCircuitBreaker) =
        circuitbreakerService.interruptExecution(joinPoint, config.service)
}
