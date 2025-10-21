package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.mapper

import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.impl.AbstractExceptionMapper
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.model.ErrorCode
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.NotAllowedSubsystemException

/**
 * Exception mapper for [NotAllowedSubsystemException].
 */
class NotAllowedSubsystemExceptionMapper : AbstractExceptionMapper<NotAllowedSubsystemException>() {

    override val mappedExceptionClass: Class<NotAllowedSubsystemException>
        get() = NotAllowedSubsystemException::class.java

    override val mappedErrorCode: ErrorCode
        get() = ErrorCode.NOT_ALLOWED_SUBSYSTEM_ERROR
}
