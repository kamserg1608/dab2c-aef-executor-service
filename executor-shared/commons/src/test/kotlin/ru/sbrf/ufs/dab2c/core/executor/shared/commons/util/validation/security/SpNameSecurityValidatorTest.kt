package ru.sbrf.ufs.dab2c.core.executor.shared.commons.util.validation.security

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.api.CallerNameExtractor
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.SpNameSecurityValidator
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.params.SpNameValidatorParameters

class SpNameSecurityValidatorTest {

    private val parameters = mockk<SpNameValidatorParameters>()
    private val callerNameExtractor = mockk<CallerNameExtractor>()

    private val validator = SpNameSecurityValidator(parameters, callerNameExtractor)

    @BeforeEach
    fun setUp() {
        every { parameters.getAllowedSubsystems() } returns DEFAULT_ALLOWED_SYSTEMS
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun testNullSpName() {
        every { callerNameExtractor.extract(any()) } returns null
        Assertions.assertThat(validator.validate("test")).isFalse()
    }

    @Test
    fun testNotAllowedSpName() {
        every { callerNameExtractor.extract(any()) } returns NOT_ALLOWED_SP_NAME
        Assertions.assertThat(validator.validate("test")).isFalse()
    }

    @Test
    fun testAllowedSpName1() {
        every { callerNameExtractor.extract(any()) } returns ALLOWED_SP_NAME_ONE
        Assertions.assertThat(validator.validate("test")).isTrue()
    }

    @Test
    fun testAllowedSpName2() {
        every { callerNameExtractor.extract(any()) } returns ALLOWED_SP_NAME_TWO
        Assertions.assertThat(validator.validate("test")).isTrue()
    }

    private companion object {
        const val NOT_ALLOWED_SP_NAME = "notAllowedSpName"
        const val ALLOWED_SP_NAME_ONE = "allowedSpName1"
        const val ALLOWED_SP_NAME_TWO = "allowedSpName2"
        val DEFAULT_ALLOWED_SYSTEMS = listOf(ALLOWED_SP_NAME_ONE, ALLOWED_SP_NAME_TWO)
    }
}
