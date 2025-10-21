package ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.model.ErrorCode
import javax.ws.rs.NotAcceptableException

/**
 * AbstractExceptionMapper implementation for NotAcceptableException.
 */
class NotAcceptableExceptionMapper : AbstractExceptionMapper<NotAcceptableException>() {

    override val mappedExceptionClass: Class<NotAcceptableException>
        get() = NotAcceptableException::class.java

    override val mappedErrorCode: ErrorCode
        get() = ErrorCode.FUNCTIONALITY_NOT_AVAILABLE
}
