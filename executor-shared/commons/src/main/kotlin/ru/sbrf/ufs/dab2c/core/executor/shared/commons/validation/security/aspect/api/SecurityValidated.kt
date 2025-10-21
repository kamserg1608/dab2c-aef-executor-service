package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api

/**
 * To be used to mark methods as security validated.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SecurityValidated(
    /**
     * Param name for allowed subsystems.
     */
    val allowedSystemsParam: String
) {

    /**
     * Used to mark method parameter as containing 'spName'.
     */
    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class SpNameHolder
}
