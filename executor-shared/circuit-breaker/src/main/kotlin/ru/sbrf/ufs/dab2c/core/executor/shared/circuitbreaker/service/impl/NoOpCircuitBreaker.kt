package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.impl

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.vavr.collection.HashMap
import io.vavr.collection.Map
import java.util.concurrent.TimeUnit

/**
 * A no-operation (NoOp) implementation of the CircuitBreaker interface.
 * This class provides a circuit breaker that does not perform any actual circuit breaking logic.
 * It can be used as a placeholder or for testing purposes where circuit breaking behavior is not required.
 */
@Suppress("TooManyFunctions")
class NoOpCircuitBreaker(
    private val name: String
) : CircuitBreaker {

    override fun tryAcquirePermission(): Boolean = true

    override fun releasePermission() {
        // NOP
    }

    override fun acquirePermission() {
        // NOP
    }

    override fun onError(duration: Long, durationUnit: TimeUnit, throwable: Throwable) {
        // NOP
    }

    override fun onSuccess(duration: Long, durationUnit: TimeUnit) {
        // NOP
    }

    override fun onResult(duration: Long, durationUnit: TimeUnit, result: Any) {
        // NOP
    }

    override fun reset() {
        // NOP
    }

    override fun transitionToClosedState() {
        // NOP
    }

    override fun transitionToOpenState() {
        // NOP
    }

    override fun transitionToHalfOpenState() {
        // NOP
    }

    override fun transitionToDisabledState() {
        // NOP
    }

    override fun transitionToMetricsOnlyState() {
        // NOP
    }

    override fun transitionToForcedOpenState() {
        // NOP
    }

    override fun getName(): String = name

    override fun getState(): CircuitBreaker.State = CircuitBreaker.State.CLOSED

    override fun getCircuitBreakerConfig(): CircuitBreakerConfig = throw UnsupportedOperationException()

    override fun getMetrics(): CircuitBreaker.Metrics = throw UnsupportedOperationException()

    override fun getTags(): Map<String, String> = HashMap.empty()

    override fun getEventPublisher(): CircuitBreaker.EventPublisher = throw UnsupportedOperationException()

    override fun getCurrentTimestamp(): Long = 0

    override fun getTimestampUnit(): TimeUnit = TimeUnit.MILLISECONDS
}
