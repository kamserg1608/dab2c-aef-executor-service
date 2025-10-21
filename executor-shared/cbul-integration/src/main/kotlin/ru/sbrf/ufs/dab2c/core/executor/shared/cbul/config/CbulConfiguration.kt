package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import ru.sbrf.cbul.client.service.CbulClientService
import ru.sbrf.cbul.client.service.CbulClientServiceImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.annotations.ProdProfileOnly
import ru.sbrf.ufs.dab2c.core.executor.shared.annotations.StubProfileOnly
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.AdditionalCbulParameters
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.CbulBulkReadDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.ManagedCbulDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.AdditionalCbulParametersImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.BatchSizeAwareCbulBulkReadDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.CbulDaoServiceImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.CbulExceptionMapper
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.CircuitBreakingCbulDaoServiceImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.ExceptionHandlingCbulDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.InMemoryCbulDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.MonitoredCbulDaoServiceImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.mapper.CbulObjectMapperProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.config.CircuitBreakerConfiguration
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.MonitoringServiceAdapter
import ru.sbrf.ufs.platform.config.v2.ExtendedConfigService

/**
 * Cbul configuration.
 */
@Configuration
@Import(
    CbulMonitoringConfiguration::class,
    CircuitBreakerConfiguration::class
)
class CbulConfiguration {

    @ProdProfileOnly
    @Bean(INITIAL_CBUL_DAO_SERVICE)
    internal fun cbulDaoService(
        cbulClientService: CbulClientService,
        localTimeProvider: LocalTimeProvider
    ): CbulBulkReadDaoService = CbulDaoServiceImpl(
        cbulClientService,
        localTimeProvider
    ) { CbulClientServiceImpl.getApiQueryFactory() }

    @StubProfileOnly
    @Bean(INITIAL_CBUL_DAO_SERVICE)
    internal fun cbulStubDaoService(
        localTimeProvider: LocalTimeProvider,
    ): ManagedCbulDaoService = InMemoryCbulDaoService(localTimeProvider, CbulObjectMapperProvider().getMapper())

    @Bean(CIRCUIT_BREAKER_CBUL_DAO_SERVICE)
    internal fun circuitBreakerCbulDaoService(
        @Qualifier(INITIAL_CBUL_DAO_SERVICE) delegate: CbulBulkReadDaoService
    ): CbulBulkReadDaoService = CircuitBreakingCbulDaoServiceImpl(delegate)

    @Bean(BATCH_AWARE_CBUL_DAO_SERVICE)
    internal fun batchAwareCbulDaoService(
        @Qualifier(CIRCUIT_BREAKER_CBUL_DAO_SERVICE) delegate: CbulBulkReadDaoService,
        additionalCbulParameters: AdditionalCbulParameters
    ): CbulBulkReadDaoService = BatchSizeAwareCbulBulkReadDaoService(delegate, additionalCbulParameters)

    @Bean(MONITORED_CBUL_DAO_SERVICE)
    internal fun monitoredCbulDaoService(
        @Qualifier(BATCH_AWARE_CBUL_DAO_SERVICE) delegate: CbulBulkReadDaoService,
        monitoringServiceAdapter: MonitoringServiceAdapter
    ): CbulBulkReadDaoService = MonitoredCbulDaoServiceImpl(delegate, monitoringServiceAdapter)

    @Bean(ERROR_HANDLING_CBUL_DAO_SERVICE)
    @Primary
    internal fun errorHandlingCbulDaoService(
        @Qualifier(MONITORED_CBUL_DAO_SERVICE)
        delegate: CbulBulkReadDaoService
    ): CbulBulkReadDaoService = ExceptionHandlingCbulDaoService(delegate)

    @Bean
    internal fun cbulExceptionMapper() = CbulExceptionMapper()

    @Bean
    internal fun additionalCbulProperties(configService: ExtendedConfigService): AdditionalCbulParameters =
        AdditionalCbulParametersImpl(configService)

    /**
     * Companion object.
     */
    companion object {
        /**
         * Initial CBUL DAO service.
         */
        const val INITIAL_CBUL_DAO_SERVICE = "initialCbulDaoService"
        private const val ERROR_HANDLING_CBUL_DAO_SERVICE = "errorHandlingCbulDaoService"
        private const val CIRCUIT_BREAKER_CBUL_DAO_SERVICE = "circuitBreakerCbulDaoService"
        private const val MONITORED_CBUL_DAO_SERVICE = "monitoredCbulDaoService"
        private const val BATCH_AWARE_CBUL_DAO_SERVICE = "batchAwareCbulDaoService"
    }
}
