package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.impl

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.impl.AbstractExceptionMapper
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.model.ErrorCode

/**
 * Implementation of AbstractExceptionMapper for CallNotPermittedException.
 */
class CallNotPermittedExceptionMapper : AbstractExceptionMapper<CallNotPermittedException>() {

    override val mappedExceptionClass: Class<CallNotPermittedException>
        get() = CallNotPermittedException::class.java

    override val mappedErrorCode: ErrorCode
        get() = ErrorCode.CALL_NOT_PERMITTED_BY_CIRCUIT_BREAKER
}
