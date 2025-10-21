package ru.sbrf.ufs.dab2c.core.executor.shared.system.env

/**
 * An interface that provides access to system environment properties.
 */
interface SystemEnvProvider {

    /**
     * Retrieves the value of the specified system property by [name].
     */
    fun getProperty(name: String): String?
}
