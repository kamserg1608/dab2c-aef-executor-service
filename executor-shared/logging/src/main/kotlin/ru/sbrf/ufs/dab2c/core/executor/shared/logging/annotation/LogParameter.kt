package ru.sbrf.ufs.dab2c.core.executor.shared.logging.annotation

/**
 * Marks variable as candidate for parameters extraction.
 * @see [PropagateLogParameters]
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogParameter
