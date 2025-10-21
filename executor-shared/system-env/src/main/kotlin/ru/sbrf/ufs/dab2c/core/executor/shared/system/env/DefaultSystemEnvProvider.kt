package ru.sbrf.ufs.dab2c.core.executor.shared.system.env

/**
 * The default implementation of the [SystemEnvProvider] interface that retrieves
 * system properties using the standard Java mechanism.
 */
class DefaultSystemEnvProvider : SystemEnvProvider {

    /**
     * Retrieves the value of the specified system property with name [name].
     */
    override fun getProperty(name: String): String? = System.getenv(name)
}
