package ru.sbrf.ufs.dab2c.core.executor.shared.logging.aspect

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.impl.RestExceptionHandlingAdvice.Companion.ORDER
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.annotation.LogParameter
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.annotation.PropagateLogParameters
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.service.api.LogParametersService
import ru.sbrf.ufs.platform.logger.LoggerContext

/**
 * Spring [Aspect] that propagate parameters from method argument to [LoggerContext].
 *
 * Ordered by [ORDER] - 101 to ensure that it executes before [SpNameValidationAspect]
 * which is ordered by [ORDER] - 100.
 */
@Aspect
@Suppress("MagicNumber")
@Order(ORDER - 101)
class LogParametersPropagatorAspect(
    private val logParametersService: LogParametersService,
) {
    /**
     * Executes on methods that are marked [PropagateLogParameters] annotation, extract parameters from method
     * arguments and pass it to [LoggerContext].
     */
    @Around("@annotation(parameters)")
    fun propagateLogParameters(
        joinPoint: ProceedingJoinPoint,
        parameters: PropagateLogParameters,
    ): Any {
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        val logParameter = method.parameters
            .zip(joinPoint.args)
            .firstOrNull { (parameter, _) -> parameter.isAnnotationPresent(LogParameter::class.java) }?.second

        check(logParameter != null) { "No parameter is annotated with @LogParameter" }

        try {
            logParametersService.processParameters(arrayOf(logParameter), parameters)
            val response = joinPoint.proceed() as ResponseEntity<*>
            return response
        } finally {
            if (parameters.clearContext) {
                logParametersService.restoreContext()
            }
        }
    }
}
