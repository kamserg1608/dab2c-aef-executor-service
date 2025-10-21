package ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.exception.UfsIdNotFoundException
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.model.ErrorCode

/** Maps [UfsIdNotFoundException] to [ErrorCode]. */
class UfsIdNotFoundExceptionMapper : AbstractExceptionMapper<UfsIdNotFoundException>() {

    override val mappedExceptionClass: Class<UfsIdNotFoundException>
        get() = UfsIdNotFoundException::class.java

    override val mappedErrorCode: ErrorCode
        get() = ErrorCode.UFS_ID_NOT_FOUND
}
