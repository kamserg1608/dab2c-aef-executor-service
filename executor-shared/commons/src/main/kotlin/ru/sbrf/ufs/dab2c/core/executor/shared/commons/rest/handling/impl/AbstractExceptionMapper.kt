package ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.api.ExceptionMapper
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.model.ErrorCode

/**
 * Abstract exception mapper.
 */
abstract class AbstractExceptionMapper<T : Throwable> : ExceptionMapper<T> {

    /**
     * Exception class.
     */
    abstract val mappedExceptionClass: Class<T>

    /**
     * Error code.
     */
    abstract val mappedErrorCode: ErrorCode

    override fun map(throwable: Any): ErrorCode? =
        if (mappedExceptionClass.isAssignableFrom(throwable.javaClass)) mappedErrorCode else null

    override fun getOrder(): Int = 0
}
