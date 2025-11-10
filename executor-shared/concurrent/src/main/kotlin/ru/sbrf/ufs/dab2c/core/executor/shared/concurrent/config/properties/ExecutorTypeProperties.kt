package ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.config.properties


import org.springframework.boot.context.properties.ConfigurationProperties
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.model.ExecutorType

/**
 * Spring configuration properties reader for web-server ExecutorType property.
 */
@ConfigurationProperties(prefix = "application.executor")
//@SuppressFBWarnings("AI_ANNOTATION_ISSUES_NEEDS_NULLABLE")
@Suppress("MagicNumber")
class ExecutorTypeProperties {

    /**
     * Type of the executor.
     */
    var type: ExecutorType = ExecutorType.DEFAULT

    /**
     * Thread pool size.
     */
    var fixedThreadPoolSize = 100
}
