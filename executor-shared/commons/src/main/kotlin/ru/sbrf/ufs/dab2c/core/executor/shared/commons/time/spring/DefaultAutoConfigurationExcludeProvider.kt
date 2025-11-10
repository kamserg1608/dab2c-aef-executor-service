package ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.spring


import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.PropertySource
import org.springframework.lang.NonNull
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL

/**
 * [EnvironmentPostProcessor] self-registering itself as [PropertySource]
 * providing customization for 'spring.autoconfigure.exclude' property via file 'autoconfiguration.excludes'.
 */
//@SuppressFBWarnings("URLCONNECTION_SSRF_FD")
class DefaultAutoConfigurationExcludeProvider : PropertySource<Array<String?>>(
    PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE,
    exclusions
), EnvironmentPostProcessor, Ordered {

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        environment.propertySources.addFirst(this)
    }

    override fun getProperty(@NonNull name: String): Any? {
        if (PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE == name) {
            return getSource()
        }
        return null
    }

    override fun containsProperty(@NonNull name: String): Boolean = PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE == name

    internal companion object {
        private const val PROPERTY_NAME_AUTOCONFIGURE_EXCLUDE = "spring.autoconfigure.exclude"
        private const val AUTOCONFIGURE_EXCLUDE_FILENAME = "autoconfiguration.excludes"

        private val logger =
            LoggerFactory.getLogger(DefaultAutoConfigurationExcludeProvider::class.java)

        private val exclusions: Array<String?>
            get() = Thread.currentThread().contextClassLoader.getResources(AUTOCONFIGURE_EXCLUDE_FILENAME)
                .asSequence()
                .map { url: URL ->
                    url.openStream().use { input ->
                        try {
                            BufferedReader(InputStreamReader(input)).readLines()
                                .asSequence()
                                .map { line -> line.trim() }
                                .filter { line -> line.isNotEmpty() }
                                .toList()
                        } catch (e: IOException) {
                            logger.error(
                                "Unable to load autoconfiguration excludes",
                                e
                            )
                            emptyList()
                        }
                    }
                }.flatMap { it.asSequence() }
                .toList()
                .toTypedArray()
    }
}
