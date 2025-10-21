package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.annotation.Order
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.impl.RestExceptionHandlingAdvice.Companion.ORDER
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.NotAllowedSubsystemException
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.SecurityValidated
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.SecurityValidatorFactory

/**
 * Aspect enabling [SecurityValidated] accompanied by [SecurityValidated.SpNameHolder].
 */
@Suppress("MagicNumber")
@Aspect
@Order(ORDER - 99)
class SpNameValidationAspect(
    private val securityValidatorFactory: SecurityValidatorFactory
) {

    /**
     * Aspect entrypoint.
     */
    @Around("@annotation(config)")
    fun intercept(joinPoint: ProceedingJoinPoint, config: SecurityValidated): Any? {
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method

        val spNameHolders =
            method.parameters.zip(joinPoint.args).filter { (parameter, value) ->
                value != null && parameter.isAnnotationPresent(SecurityValidated.SpNameHolder::class.java)
            }.map { it.second!! }.toList()
        require(spNameHolders.size == 1) { "SpNameHolder annotation can be applied only to one parameter!" }

        validate(config.allowedSystemsParam, spNameHolders[0])
        return joinPoint.proceed()
    }

    private fun validate(allowedSystemsParameter: String, value: Any) {

        val valid = securityValidatorFactory.create(allowedSystemsParameter).validate(value)
        if (!valid) throw NotAllowedSubsystemException("Вызов запрещен!")
    }
}
