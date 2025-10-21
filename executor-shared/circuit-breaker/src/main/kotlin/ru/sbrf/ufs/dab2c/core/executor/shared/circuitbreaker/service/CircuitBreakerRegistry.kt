package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service

import io.github.resilience4j.circuitbreaker.CircuitBreaker

/**
 * Interface for managing circuit breakers in a registry.
 * This interface defines the contract for retrieving circuit breakers based on service names.
 */
interface CircuitBreakerRegistry {

    /**
     * Retrieves a circuit breaker from the registry based on the provided service name.
     *
     * If a circuit breaker is already registered for the specified service name, it is returned.
     * Otherwise, a new circuit breaker is created using the appropriate parameters and registered in the registry.
     */
    fun getCircuitBreaker(serviceName: String): CircuitBreaker

    /**
     * Removes all elements from registry.
     */
    fun clearRegistry()
}
