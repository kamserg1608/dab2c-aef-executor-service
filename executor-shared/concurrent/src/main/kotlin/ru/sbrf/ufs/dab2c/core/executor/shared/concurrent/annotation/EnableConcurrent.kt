package ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.annotation

import org.springframework.context.annotation.Import
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.config.ConcurrentConfiguration
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS

/**
 * Imports [ConcurrentConfiguration] and related beans,
 * allows to handle Executors-pool and other related application parameters.
 */
@Target(CLASS)
@Retention(RUNTIME)
@Import(value = [ConcurrentConfiguration::class])
annotation class EnableConcurrent
