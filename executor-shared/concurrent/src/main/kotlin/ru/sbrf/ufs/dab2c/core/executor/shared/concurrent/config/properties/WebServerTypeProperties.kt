package ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.config.properties

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.springframework.boot.context.properties.ConfigurationProperties
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.model.ExecutorType

/**
 * Spring configuration properties reader for web-server ExecutorType property.
 */
@ConfigurationProperties(prefix = "web.server.executor")
@SuppressFBWarnings("AI_ANNOTATION_ISSUES_NEEDS_NULLABLE")
@Suppress("MagicNumber")
class WebServerTypeProperties {

    /**
     * Webserver type.
     */
    var type: ExecutorType = ExecutorType.DEFAULT

    /**
     * Thread pool size.
     */
    var fixedThreadPoolSize = 100
}
