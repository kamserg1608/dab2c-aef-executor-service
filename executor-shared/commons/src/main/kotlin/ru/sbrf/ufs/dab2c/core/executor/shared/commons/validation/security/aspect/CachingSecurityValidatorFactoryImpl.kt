package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect

import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.SecurityValidator
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.SecurityValidatorFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Default implementation of [SecurityValidatorFactory] caching already retrieved validators.
 */
class CachingSecurityValidatorFactoryImpl(
    private val delegate: SecurityValidatorFactory
) : SecurityValidatorFactory {

    private val cache = ConcurrentHashMap<String, SecurityValidator>()

    override fun create(subject: String): SecurityValidator =

        cache.computeIfAbsent(subject) {
            delegate.create(subject)
        }
}
