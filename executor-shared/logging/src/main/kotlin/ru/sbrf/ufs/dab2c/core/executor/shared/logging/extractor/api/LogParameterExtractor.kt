package ru.sbrf.ufs.dab2c.core.executor.shared.logging.extractor.api

import ru.sbrf.ufs.dab2c.core.executor.shared.logging.aspect.LogParametersPropagatorAspect
import ru.sbrf.ufs.platform.logger.LoggerContext

/**
 * Extractor for log parameters from given object. It used in [LogParametersPropagatorAspect] for mdc extracting.
 */
interface LogParameterExtractor<T> {

    /**
     * Supported class type for extracting.
     */
    val supportedType: Class<T>

    /**
     * Extracts parameters for [LoggerContext] from given argument.
     */
    fun extractLoggingParams(arg: T): Map<String, String>
}
