package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.impl.AbstractExceptionMapper
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.model.ErrorCode
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.model.ValidationException

/** Maps [ValidationException] to [ErrorCode]. */
class ValidationExceptionMapper : AbstractExceptionMapper<ValidationException>() {

    override val mappedExceptionClass: Class<ValidationException>
        get() = ValidationException::class.java

    override val mappedErrorCode: ErrorCode
        get() = ErrorCode.VALIDATION_ERROR
}
