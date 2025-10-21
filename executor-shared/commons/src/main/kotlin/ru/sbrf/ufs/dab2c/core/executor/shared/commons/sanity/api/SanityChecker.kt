package ru.sbrf.ufs.dab2c.core.executor.shared.commons.sanity.api

/**
 * Represents a sanity checker.
 */
interface SanityChecker {
    /**
     * Checks the sanity of configuration.
     * @throws RuntimeException if sanity check failed
     */
    fun check()
}
