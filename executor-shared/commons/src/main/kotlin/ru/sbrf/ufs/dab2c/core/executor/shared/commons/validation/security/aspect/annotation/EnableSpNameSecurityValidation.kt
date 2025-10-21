package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.annotation

import org.springframework.context.annotation.Import
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.SecurityValidated
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.config.SpNameValidationConfiguration
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS

/**
 * Enables security validation for classes annotated with [SecurityValidated].
 *
 * This annotation imports the [SpNameValidationConfiguration] class,
 * which contains the necessary configuration for validation to be
 * applied.
 */
@Target(CLASS)
@Retention(RUNTIME)
@Import(value = [SpNameValidationConfiguration::class])
annotation class EnableSpNameSecurityValidation
