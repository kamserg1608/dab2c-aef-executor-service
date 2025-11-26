package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.util

import javax.annotation.Nonnull
import java.lang.reflect.UndeclaredThrowableException

/**
 * Utility class for methods used to extract reportable throwable out of other throwables.
 */
object ReportableThrowableUtils {

    /**
     * Extracts reportable throwable out of given [throwable].
     */
    // @SuppressFBWarnings("PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
    fun getReportableThrowable(@Nonnull throwable: Throwable): Throwable {
        if (throwable is UndeclaredThrowableException && throwable.cause != null) {
            return throwable.cause!!
        }

        return throwable
    }
}
