package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.impl

import org.aspectj.lang.ProceedingJoinPoint
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.CircuitBreakerRegistry
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.CircuitBreakerService
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.util.ReportableThrowableUtils
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider
import java.util.concurrent.TimeUnit

/**
 * Implementation of the CircuitBreakerService interface.
 * This class provides the logic for interrupting method execution based on circuit breaker states.
 */
@Suppress("TooGenericExceptionCaught")
class CircuitBreakerServiceImpl(
    private val circuitbreakerRegistry: CircuitBreakerRegistry,
    private val localTimeProvider: LocalTimeProvider
) : CircuitBreakerService {

    /**
     * Interrupts the execution of a method based on the circuit breaker logic.
     *
     * This method allows for the interception of a method execution, applying circuit breaker rules
     * to determine whether to proceed with the execution or not. The decision is based on the state
     * of the circuit breaker associated with the provided service name.
     */
    override fun interruptExecution(joinPoint: ProceedingJoinPoint, serviceName: String): Any? {

        val executionStart = localTimeProvider.currentTimeMillis()
        val executionDuration = { localTimeProvider.currentTimeMillis() - executionStart }
        val circuitBreaker = circuitbreakerRegistry.getCircuitBreaker(serviceName)

        circuitBreaker.acquirePermission()

        try {
            val result = joinPoint.proceed()
            circuitBreaker.onSuccess(executionDuration.invoke(), TimeUnit.MILLISECONDS)
            return result
        } catch (exception: Exception) {
            val throwableReport = ReportableThrowableUtils.getReportableThrowable(exception)
            circuitBreaker.onError(executionDuration.invoke(), TimeUnit.MILLISECONDS, throwableReport)
            throw exception
        }
    }
}
