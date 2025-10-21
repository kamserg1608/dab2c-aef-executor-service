package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.annotation

import org.springframework.context.annotation.Import
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.configuration.ValidationConfiguration
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS

/**
 * Indicates that validation should be enabled for the annotated class.
 *
 * This annotation imports the [ValidationConfiguration] class,
 * which contains the necessary configuration for validation to be
 * applied.
 */
@Target(CLASS)
@Retention(RUNTIME)
@Import(value = [ValidationConfiguration::class])
annotation class EnableValidation
