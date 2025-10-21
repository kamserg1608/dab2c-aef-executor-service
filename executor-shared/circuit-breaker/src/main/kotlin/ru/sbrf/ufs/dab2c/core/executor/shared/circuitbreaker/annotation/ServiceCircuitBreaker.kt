package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.annotation

/**
 * This annotation is to be used to declare service methods
 * which must be wrapped with circuitbreaker.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class ServiceCircuitBreaker(

    /**
     * Used to provide service name to circuitbreaker.
     */
    val service: String

)
