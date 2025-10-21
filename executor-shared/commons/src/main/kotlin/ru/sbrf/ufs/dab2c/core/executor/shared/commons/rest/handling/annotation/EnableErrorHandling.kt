package ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.annotation

import org.springframework.context.annotation.Import
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.configuration.ErrorHandlingConfiguration
import kotlin.annotation.AnnotationRetention.RUNTIME

/**
 * Enables error handling by importing [ErrorHandlingConfiguration].
 *
 * This annotation should be applied to classes where error handling
 * capabilities are required.
 */
@Target(AnnotationTarget.CLASS)
@Retention(RUNTIME)
@Import(value = [ErrorHandlingConfiguration::class])
annotation class EnableErrorHandling
