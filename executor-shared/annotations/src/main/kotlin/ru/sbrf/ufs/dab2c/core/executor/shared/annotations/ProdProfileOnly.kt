package ru.sbrf.ufs.dab2c.core.executor.shared.annotations

import org.springframework.context.annotation.Profile
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Indicates that a bean should only be instantiated when the "STUB" profile is NOT active.
 *
 * This annotation serves as a specialized marker for beans that are intended
 * to be used in a production environment. When the "STUB" profile is not active,
 * the Spring context will instantiate the bean annotated with this annotation.
 *
 * @see org.springframework.context.annotation.Profile
 */
@Target(FUNCTION)
@Retention(RUNTIME)
@Profile("!STUB")
annotation class ProdProfileOnly
