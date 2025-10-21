package ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.postprocessor

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.extensions.isMock
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.postprocessor.api.SpykBeanConfig

@SuppressFBWarnings("IICU_INCORRECT_INTERNAL_CLASS_USE", "NP_NULL_ON_SOME_PATH", "NP_NULL_PARAM_DEREF")
class SpykBeanPostProcessorTest {

    private lateinit var config: SpykBeanConfig
    private lateinit var processor: SpykBeanPostProcessor

    @BeforeEach
    fun setUp() {
        config = mockk(relaxed = true)
        processor = SpykBeanPostProcessor(config)
    }

    @Test
    fun `should return bean if its already a spy`() {
        val bean = spyk<TestBean>()
        every { config.spyBeanNames } returns listOf(TEST_BEAN_NAME)
        val result = processor.postProcessAfterInitialization(bean, TEST_BEAN_NAME)

        assertSame(bean, result)
    }

    @Test
    fun `should spy bean if bean name matches spyBeanNames`() {
        val bean = TestBean()
        every { config.spyBeanNames } returns listOf(TEST_BEAN_NAME)
        val result = processor.postProcessAfterInitialization(bean, TEST_BEAN_NAME)

        assert(result.isMock)
    }

    @Test
    fun `should spy bean if bean matches spyClasses`() {
        val bean = TestBean()
        every { config.spyClasses } returns listOf(TestBean::class)
        val result = processor.postProcessAfterInitialization(bean, TEST_BEAN_NAME)

        assert(result.isMock)
    }

    @Test
    fun `should return bean if it doesnt match and spy or mock condition`() {
        val bean = TestBean()
        every { config.spyBeanNames } returns emptyList()
        every { config.spyClasses } returns emptyList()
        val result = processor.postProcessAfterInitialization(bean, TEST_BEAN_NAME)

        assertSame(bean, result)
    }

    @Test
    fun `should return bean`() {
        val bean = mockk<Any>()

        val result = processor.postProcessBeforeInitialization(bean, TEST_BEAN_NAME)

        assertSame(bean, result)
    }

    @Test
    fun `should throw exception for anonymous class`() {
        val bean = object : TestBean() {}
        every { config.spyClasses } returns listOf(TestBean::class)

        val exception = assertThrows<IllegalArgumentException> {
            processor.postProcessAfterInitialization(bean, TEST_BEAN_NAME)
        }

        assertEquals("Anonymous classes are not supported: $TEST_BEAN_NAME", exception.message)
    }

    @Test
    fun `should throw exception for anonymous class when matching bean name`() {
        val bean = object : TestBean() {}
        every { config.spyBeanNames } returns listOf(TEST_BEAN_NAME)

        val exception = assertThrows<IllegalArgumentException> {
            processor.postProcessAfterInitialization(bean, TEST_BEAN_NAME)
        }

        assertEquals("Anonymous classes are not supported: $TEST_BEAN_NAME", exception.message)
    }

    @Test
    fun `should spy if object matches spyClasses`() {
        val bean = TestObject
        every { config.spyClasses } returns listOf(TestObject::class)

        val result = processor.postProcessAfterInitialization(bean, TEST_BEAN_NAME)
        assert(result.isMock)
    }

    @Test
    fun `should spy if object matches spyBeanNames`() {
        val bean = TestObject
        every { config.spyBeanNames } returns listOf(TEST_BEAN_NAME)

        val result = processor.postProcessAfterInitialization(bean, TEST_BEAN_NAME)
        assert(result.isMock)
    }

    private open class TestBean

    object TestObject

    companion object {
        private const val TEST_BEAN_NAME = "mySuperBean"
    }
}
