package ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.annotation

/**
 * This annotation is to be used to declare service methods
 * which must be monitored.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class CbulMonitored(

    /** Used to provide service to monitoring service. */
    val action: String

)
