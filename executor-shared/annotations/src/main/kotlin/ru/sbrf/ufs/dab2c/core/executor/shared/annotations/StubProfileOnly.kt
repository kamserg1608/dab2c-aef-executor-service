package ru.sbrf.ufs.dab2c.core.executor.shared.annotations

import org.springframework.context.annotation.Profile
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Indicates that a bean should only be instantiated when the "STUB" profile is active.
 *
 * This annotation serves as a specialized marker for beans that are intended
 * to be used in a stub or test environment. When the "STUB" profile is active,
 * the Spring context will instantiate the bean annotated with this annotation.
 *
 * @see org.springframework.context.annotation.Profile
 */
@Target(FUNCTION)
@Retention(RUNTIME)
@Profile("STUB")
annotation class StubProfileOnly
