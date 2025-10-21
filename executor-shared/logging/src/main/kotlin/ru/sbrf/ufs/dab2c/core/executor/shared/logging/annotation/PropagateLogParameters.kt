package ru.sbrf.ufs.dab2c.core.executor.shared.logging.annotation

import ru.sbrf.ufs.dab2c.core.executor.shared.logging.extractor.api.LogParameterExtractor
import ru.sbrf.ufs.platform.logger.LoggerContext

/**
 * Marks method as logging parameter propagation candidate.
 * If there is suitable [LogParameterExtractor] for method argument marked as [LogParameter],
 * log parameters will be extracted and added to [LoggerContext]
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class PropagateLogParameters(
    /**
     * Service name.
     */
    val service: String,
    /**
     * Clear context before propagation.
     */
    val clearContext: Boolean = false,
)
