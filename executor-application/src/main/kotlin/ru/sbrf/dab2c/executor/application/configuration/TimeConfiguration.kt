package ru.sbrf.dab2c.executor.application.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.library.time.SystemTimeProvider
import ru.sbrf.dab2c.executor.library.time.TimeProvider

/** Provides [TimeProvider] bean for the application. */
@Configuration
class TimeConfiguration {

    /** Production [TimeProvider] backed by [SystemTimeProvider]. */
    @Bean
    fun timeProvider(): TimeProvider = SystemTimeProvider()
}
