package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.model

import java.time.Duration

/** Circuit breaker configuration SUP config. */
data class PmsCircuitBreakerConfig(

    /** Service name corresponding to service being wrapped with circuit breaker. */
    var serviceName: String,

    /** True, if circuit breaker enabled, false - otherwise. */
    var isEnabled: Boolean = false,

    /** Service failure rate threshold when circuit breaker should open. */
    var failureRateThreshold: Float? = null,

    /** Set of errors which should be recorded as critical and should be counted in failure rate. */
    var recordedExceptions: Set<Class<out Throwable>> = emptySet(),

    /** Set of errors which should not be recorded as critical and should not be counted in failure rate. */
    var ignoredExceptions: Set<Class<out Throwable>> = emptySet(),

    /** Permitted number of calls in half open state. */
    var permittedNumberOfCallsInHalfOpenState: Int? = null,

    /** Ring buffer for closed state size. */
    var slidingWindow: Int? = null,

    /** Wait duration in open state before circuit breaker change it's state to. */
    var waitInOpenStateDuration: Duration? = null,

)
