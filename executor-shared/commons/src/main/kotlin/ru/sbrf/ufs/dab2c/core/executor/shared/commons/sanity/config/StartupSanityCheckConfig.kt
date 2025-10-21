package ru.sbrf.ufs.dab2c.core.executor.shared.commons.sanity.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.sanity.StartupSanityChecker
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.sanity.api.SanityChecker

/**
 * Enables sanity checks.
 */
@Configuration
class StartupSanityCheckConfig {

    @Bean
    internal fun sanityCheck(checks: List<SanityChecker>) =
        StartupSanityChecker(checks)
}
