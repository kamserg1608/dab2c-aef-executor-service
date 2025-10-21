package ru.sbrf.ufs.dab2c.core.executor.shared.commons.util.validation.security.aspect

import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.NotAllowedSubsystemException
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.SecurityValidator
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.SecurityValidatorFactory

private const val TEST_SERVICE_NAME = "test"

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [SpNameValidationAspectTestConfiguration::class])
class SpNameValidationAspectTest {

    @Autowired
    private lateinit var validatedService: SpNameValidatedService

    @Autowired
    private lateinit var securityValidatorFactory: SecurityValidatorFactory

    @Autowired
    private lateinit var securityValidator: SecurityValidator

    @BeforeEach
    fun setUp() {
        every { securityValidatorFactory.create(TEST_SERVICE_NAME) } returns securityValidator
    }

    @Test
    fun proxyIsApplied() {
        assertThat(AopUtils.isAopProxy(validatedService)).isTrue()
    }

    @Test
    fun okWhenValid() {
        every { securityValidator.validate(any()) } returns true

        assertThat(
            validatedService.validatedMethod(TEST_SERVICE_NAME)
        ).isEqualTo(TEST_SERVICE_NAME)
    }

    @Test
    fun errorWhenInvalid() {
        every { securityValidator.validate(any()) } returns false

        assertThatCode {
            validatedService.validatedMethod(TEST_SERVICE_NAME)
        }.isInstanceOf(NotAllowedSubsystemException::class.java)
    }
}
