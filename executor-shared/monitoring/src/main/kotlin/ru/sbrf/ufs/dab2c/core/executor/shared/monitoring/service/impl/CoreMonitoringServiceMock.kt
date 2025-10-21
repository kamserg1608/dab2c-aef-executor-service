@file:Suppress("DEPRECATION")

package ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.impl

import ru.sbrf.ufs.platform.logger.LoggerFactory
import ru.sbrf.ufs.platform.monitoring.CoreMonitoringService
import ru.sbrf.ufs.platform.monitoring.attributes.MetricAttributes
import ru.sbrf.ufs.platform.monitoring.ssl.SslConfig
import ru.sbrf.ufs.platform.monitoring.ssl.UpdateSsl
import java.util.function.Supplier

/**
 * Realisation of [CoreMonitoringService] that do nothing.
 */
@Suppress("TooManyFunctions")
class CoreMonitoringServiceMock : CoreMonitoringService {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun reportEvent(name: String?, value: Double, path: String?) {
        logger.debug("MonitoringServiceMock::reportEvent {} {} {}", name, value, path)
    }

    override fun reportEvent(name: String?, value: Double, path: String?, channel: String?) {
        logger.debug("MonitoringServiceMock::reportEvent channel {} {} {} {}", name, value, path, channel)
    }

    override fun reportEvent(name: String?, value: Double, path: String?, attributes: MetricAttributes?) {
        logger.debug("MonitoringServiceMock::reportEvent attributes {} {} {} {}", name, value, path, attributes)
    }

    override fun updateSslConfigKafka(kafkaConfigSupplier: Supplier<SslConfig>?): UpdateSsl.ResponseUpdateSsl? = null

    override fun updateSslConfigKafka(): UpdateSsl.ResponseUpdateSsl? = null

    override fun updateSslConfigHttp(httpConfigSupplier: Supplier<SslConfig>?): UpdateSsl.ResponseUpdateSsl? = null

    override fun updateSslConfigHttp(): UpdateSsl.ResponseUpdateSsl? = null

    override fun updateSslConfigEndpoint(endpointConfigSupplier: Supplier<SslConfig>?) = null

    override fun updateSslConfigEndpoint(): UpdateSsl.ResponseUpdateSsl? = null

    override fun updateSslConfigPushGateway(pushGatewayConfigSupplier: Supplier<SslConfig>?) = null

    override fun updateSslConfigPushGateway(): UpdateSsl.ResponseUpdateSsl? = null

    override fun updateSslConfigHttpFilters(httpFiltersConfigSupplier: Supplier<SslConfig>?) = null

    override fun updateSslConfigHttpFilters(): UpdateSsl.ResponseUpdateSsl? = null

    override fun updateSsl(): UpdateSsl.ResponseUpdateSsl? = null
}
