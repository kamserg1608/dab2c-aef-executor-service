package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service

import org.aspectj.lang.ProceedingJoinPoint

/**
 * Interface for services that handle circuit breaker logic.
 * This interface defines the contract for interrupting execution based on circuit breaker states.
 */
interface CircuitBreakerService {

    /**
     * Interrupts the execution of a method based on the circuit breaker logic.
     *
     * This method allows for the interception of a method execution, applying circuit breaker rules
     * to determine whether to proceed with the execution or not. The decision is based on the state
     * of the circuit breaker associated with the provided service name.
     */
    fun interruptExecution(joinPoint: ProceedingJoinPoint, serviceName: String): Any?
}
