package ru.sbrf.ufs.dab2c.core.executor.shared.logging.extractor.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.logging.extractor.api.LogParameterExtractor
import ru.sbrf.ufs.platform.logger.Params.USER_LOGIN

/**
 * Skeleton implementation for ucpId/tb request extraction.
 */
abstract class AbstractLogParameterExtractor<T> : LogParameterExtractor<T> {

    override fun extractLoggingParams(arg: T): Map<String, String> {
        if (!supportedType.isInstance(arg)) {
            return emptyMap()
        }

        val loggingParams: MutableMap<String, String> = mutableMapOf()
        loggingParams[USER_LOGIN] = getUcpId(arg).toString()

        return loggingParams
    }

    /**
     * Obtain ucpId from request.
     */
    abstract fun getUcpId(target: T): String?
}
