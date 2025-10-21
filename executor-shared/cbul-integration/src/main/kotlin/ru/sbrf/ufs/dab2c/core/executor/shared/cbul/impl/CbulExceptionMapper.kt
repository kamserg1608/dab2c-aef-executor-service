package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.exceptions.CbulException
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.impl.AbstractExceptionMapper
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.model.ErrorCode

/** Maps [CbulException] to [ErrorCode]. */
class CbulExceptionMapper : AbstractExceptionMapper<CbulException>() {

    override val mappedExceptionClass: Class<CbulException>
        get() = CbulException::class.java

    override val mappedErrorCode: ErrorCode
        get() = ErrorCode.CBUL_ERROR
}
