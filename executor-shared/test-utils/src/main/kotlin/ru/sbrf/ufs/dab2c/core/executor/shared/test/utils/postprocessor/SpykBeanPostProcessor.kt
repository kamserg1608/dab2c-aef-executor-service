package ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.postprocessor

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.mockk.mockkObject
import io.mockk.spyk
import org.springframework.beans.factory.config.BeanPostProcessor
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.extensions.isMock
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.postprocessor.api.SpykBeanConfig

/**
 * Applies spies or mocks to beans based on configuration.
 *
 * @param config Configuration containing names and classes of beans to be spied or mocked.
 */
@SuppressFBWarnings("IICU_INCORRECT_INTERNAL_CLASS_USE", "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS")
class SpykBeanPostProcessor(
    private val config: SpykBeanConfig
) : BeanPostProcessor {

    /**
     * Returns original bean without any modifications.
     */
    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any = bean

    /**
     * Determines whether to apply spy or mock based on configuration.
     */
    override fun postProcessAfterInitialization(bean: Any, beanName: String) =
        when {
            bean.isMock -> bean
            config.spyBeanNames.contains(beanName) -> processBean(bean, beanName)
            config.spyClasses.any { it.isInstance(bean) } -> processBean(bean, beanName)
            else -> bean
        }

    private fun processBean(bean: Any, beanName: String) =
        if (isObject(bean)) {
            mockkObject(bean)
            bean
        } else {
            wrapWithSpy(bean, beanName)
        }

    private fun wrapWithSpy(bean: Any, beanName: String): Any {
        require(!bean::class.java.isAnonymousClass) {
            "Anonymous classes are not supported: $beanName"
        }
        return spyk(bean)
    }

    private fun isObject(bean: Any) = bean::class.java.kotlin.objectInstance != null
}
