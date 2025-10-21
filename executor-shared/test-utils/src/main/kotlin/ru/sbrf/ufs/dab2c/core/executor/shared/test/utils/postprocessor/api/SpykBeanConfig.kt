package ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.postprocessor.api

import kotlin.reflect.KClass

/**
 * Maintains lists of bean names and classes to be spied or mocked.
 */
data class SpykBeanConfig(
    /**
     * Bean names to which spies will be applied.
     */
    val spyBeanNames: List<String> = emptyList(),

    /**
     * Classes whose instances should be wrapped with spies.
     */
    val spyClasses: List<KClass<*>> = emptyList()
)
