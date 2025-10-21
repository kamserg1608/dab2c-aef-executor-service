package ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.DefaultLocalTimeProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider

/**
 * Configuration of LocalTimeProvider.
 */
@Configuration
class LocalTimeConfiguration {

    @Bean
    internal fun localTimeProvider(): LocalTimeProvider = DefaultLocalTimeProvider()
}
