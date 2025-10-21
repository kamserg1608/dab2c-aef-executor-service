package ru.sbrf.ufs.dab2c.core.executor.shared.commons.util.validation.security.params

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.params.SpNameValidatorParametersImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.config.ExtendedConfigServiceFactory

class SpNameValidatorParametersImplTest {

    private val testConfigService = ExtendedConfigServiceFactory.buildFileBased(
        "/validation/security/test-sp-name-sup-config.json"
    )

    @Test
    fun presentTest() {

        val params = SpNameValidatorParametersImpl(testConfigService, "test.prefix.test3")
        assertThat(params.getAllowedSubsystems())
            .containsExactlyInAnyOrder("test4", "test5")
    }

    @Test
    fun emptyTest() {

        val params = SpNameValidatorParametersImpl(testConfigService, "test.prefix.test4")
        assertThat(params.getAllowedSubsystems()).isEmpty()
    }

    @Test
    fun notPresentTest() {

        val params = SpNameValidatorParametersImpl(testConfigService, "test.prefix.test5")
        assertThat(params.getAllowedSubsystems()).isEmpty()
    }
}
