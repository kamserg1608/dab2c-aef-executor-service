package ru.sbrf.ufs.dab2c.core.executor.shared.commons.util.validation.security.aspect

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.api.CallerNameExtractor
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.SecurityValidatorFactoryImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.config.ExtendedConfigServiceFactory
import java.util.stream.Stream

class SecurityValidatorFactoryImplTest {

    private val testConfigService = ExtendedConfigServiceFactory.buildFileBased(
        "/validation/security/test-sp-name-sup-config.json"
    )

    fun validationTestData() = Stream.of(
        arguments("test.prefix.test", "test", true),
        arguments("test.prefix.test", "test2", false),
        arguments("test.prefix.test2", "test3", true)
    )

    @MethodSource("validationTestData")
    @ParameterizedTest
    fun validationTest(parameterName: String, spName: String, expectedResult: Boolean) {

        val callerNameExtractor = mockk<CallerNameExtractor>()
        every { callerNameExtractor.extract(any()) } answers { it.invocation.args[0] as String }

        val factory = SecurityValidatorFactoryImpl(
            testConfigService,
            callerNameExtractor
        )

        val validator = factory.create(parameterName)

        assertThat(validator.validate(spName)).isEqualTo(expectedResult)
    }
}
