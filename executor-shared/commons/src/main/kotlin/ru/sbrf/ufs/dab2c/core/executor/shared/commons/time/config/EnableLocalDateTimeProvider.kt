package ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.config

import org.springframework.context.annotation.Import
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider

/**
 * Enables [LocalTimeProvider].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(LocalTimeConfiguration::class)
annotation class EnableLocalDateTimeProvider
