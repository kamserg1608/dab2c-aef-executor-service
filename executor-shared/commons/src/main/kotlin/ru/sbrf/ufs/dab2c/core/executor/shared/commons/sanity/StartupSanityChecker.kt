package ru.sbrf.ufs.dab2c.core.executor.shared.commons.sanity

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.sanity.api.SanityChecker

/**
 * Performs startup sanity checks.
 */
@Suppress("TooGenericExceptionCaught")
class StartupSanityChecker(
    /**
     * List of checkers.
     */
    private val checkers: List<SanityChecker>
) : InitializingBean {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun afterPropertiesSet() {
        val results = checkers.map { check(it) }.filter { !it.result }
        check(results.isEmpty()) {
            results.joinToString("\n") { it.message!! }
        }
    }

    private fun check(check: SanityChecker): SanityCheckResult {
        try {
            check.check()
            return SanityCheckResult(true)
        } catch (e: RuntimeException) {
            logger.error("Sanity check failed", e)
            return SanityCheckResult(false, e.message)
        }
    }

    private data class SanityCheckResult(
        val result: Boolean,
        val message: String? = null
    )
}
