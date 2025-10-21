package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.impl

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.converter.CircuitBreakerConfigConverter
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.CircuitBreakerParameters
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.model.PmsCircuitBreakerConfig
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.CircuitBreakerRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicReference

/**
 * Implementation of the [CircuitBreakerRegistry] interface, responsible for managing a registry of circuit breakers.
 */
class CircuitBreakerRegistryImpl(
    private val circuitBreakerParameters: CircuitBreakerParameters
) : CircuitBreakerRegistry {

    /**
     * A concurrent map that stores the circuit breakers, keyed by their unique identifiers.
     */
    private val circuitBreakerRegistry: ConcurrentMap<String, AtomicReference<CircuitBreaker>> = ConcurrentHashMap()

    /**
     * Removes all elements from registry.
     */
    override fun clearRegistry() = circuitBreakerRegistry.clear()

    /**
     * Retrieves a circuit breaker from the registry based on the provided service name.
     */
    override fun getCircuitBreaker(serviceName: String): CircuitBreaker = circuitBreakerRegistry
        .getOrPut(serviceName) { createCircuitBreaker(serviceName) }
        .get()

    private fun createCircuitBreaker(serviceName: String): AtomicReference<CircuitBreaker> {
        val pmsCircuitBreakerConfig = circuitBreakerParameters.getParameters(serviceName)
        val atomicCircuitBreaker = AtomicReference(makeCircuitBreaker(pmsCircuitBreakerConfig))
        circuitBreakerParameters.subscribe(serviceName) { mutateCircuitBreaker(serviceName) }
        return atomicCircuitBreaker
    }

    private fun makeCircuitBreaker(config: PmsCircuitBreakerConfig): CircuitBreaker {

        if (!config.isEnabled) return NoOpCircuitBreaker(config.serviceName)

        val circuitBreakerConfig = CircuitBreakerConfigConverter.convert(config)
        val circuitBreaker: CircuitBreaker = CircuitBreaker.of(config.serviceName, circuitBreakerConfig)
        circuitBreaker.eventPublisher.onEvent(LoggingEventConsumerImpl())

        return circuitBreaker
    }

    private fun mutateCircuitBreaker(serviceName: String) =
        circuitBreakerRegistry[serviceName]!!
            .set(makeCircuitBreaker(circuitBreakerParameters.getParameters(serviceName)))
}
