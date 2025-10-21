package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.converter

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.model.PmsCircuitBreakerConfig

/**
 * Converts [PmsCircuitBreakerConfig] into [CircuitBreakerConfig].
 */
@Suppress("SpreadOperator")
object CircuitBreakerConfigConverter {

    /**
     * Converts [PmsCircuitBreakerConfig] into [CircuitBreakerConfig].
     */
    fun convert(config: PmsCircuitBreakerConfig): CircuitBreakerConfig =
        CircuitBreakerConfig.custom()
            .writableStackTraceEnabled(false)
            .failureRateThreshold(config.failureRateThreshold!!)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .slidingWindow(config.slidingWindow!!, config.slidingWindow!!, COUNT_BASED)
            .permittedNumberOfCallsInHalfOpenState(config.permittedNumberOfCallsInHalfOpenState!!)
            .waitDurationInOpenState(config.waitInOpenStateDuration!!)
            .recordExceptions(*config.recordedExceptions.toTypedArray())
            .ignoreExceptions(*config.ignoredExceptions.toTypedArray())
            .recordException { false }
            .build()
}
