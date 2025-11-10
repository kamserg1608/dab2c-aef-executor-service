package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.sanity.config.StartupSanityCheckConfig
import ru.sbrf.ufs.dab2c.core.executor.shared.healthcheck.HealthCheckConfiguration
import ru.sbrf.ufs.dab2c.core.executor.shared.rest.mapper.configuration.RestObjectMapperConfiguration

/**
 * Root configuration. Used to accumulate all application configurations.
 */
@Configuration
@Import(
    value = [
        ServiceConfiguration::class,
        HealthCheckConfiguration::class,
        RestConfiguration::class,
        RestObjectMapperConfiguration::class,
        StartupSanityCheckConfig::class
    ]
)
class RootConfiguration
