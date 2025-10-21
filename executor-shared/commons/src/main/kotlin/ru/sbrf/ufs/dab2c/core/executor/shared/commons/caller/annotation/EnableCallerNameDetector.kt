package ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.annotation

import org.springframework.context.annotation.Import
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.configuration.CallerNameExtractorChainConfiguration
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS

/**
 * Imports [CallerNameDetector] and related beans.
 */
@Target(CLASS)
@Retention(RUNTIME)
@Import(value = [CallerNameExtractorChainConfiguration::class])
annotation class EnableCallerNameDetector
