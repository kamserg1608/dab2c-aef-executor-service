package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.helpers

import java.util.Locale

/**
 * Вспомогательный класс для извлечения свойств в тестах.
 *
 */
object PropertiesHelper {

    @JvmStatic
    val baseUrl: String
        get() {
            val host = System.getProperty("server.host")
            val port = System.getProperty("server.port")
            val warContextPath = System.getProperty("server.contextPath")
            return String.format(Locale.US, "http://%s:%s/%s", host, port, warContextPath)
        }
}
