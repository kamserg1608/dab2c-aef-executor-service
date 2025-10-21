package ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.api

import org.springframework.core.Ordered
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.model.ErrorCode

/** Allows to map [T] to specific [ErrorCode]. */
interface ExceptionMapper<T : Throwable> : Ordered {

    /** Returns [ErrorCode] related to [Throwable] or null if [Throwable] cant be mapped. */
    fun map(throwable: Any): ErrorCode?
}
