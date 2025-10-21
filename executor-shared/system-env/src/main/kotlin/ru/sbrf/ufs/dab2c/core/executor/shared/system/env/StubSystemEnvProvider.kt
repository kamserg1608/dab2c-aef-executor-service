package ru.sbrf.ufs.dab2c.core.executor.shared.system.env

/**
 * A stub implementation of the [SystemEnvProvider] interface that provides
 * a way to simulate system environment properties for testing purposes.
 */
class StubSystemEnvProvider(
    private val env: Map<String, String>
) : SystemEnvProvider {

    /**
     * Retrieves the value of the specified environment property from the provided map.
     */
    override fun getProperty(name: String): String? = env[name]
}
