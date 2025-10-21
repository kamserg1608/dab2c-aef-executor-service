package ru.sbrf.ufs.dab2c.core.executor.shared.commons.util.validation.security.aspect

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.CachingSecurityValidatorFactoryImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.SecurityValidator
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.SecurityValidatorFactory

class CachingSecurityValidatorFactoryImplTest {

    @Test
    fun cachesValues() {

        val originalFactory = mockk<SecurityValidatorFactory>()
        every { originalFactory.create("test") } answers { mockk<SecurityValidator>() }

        val cachingFactory = CachingSecurityValidatorFactoryImpl(originalFactory)

        val validator1 = cachingFactory.create("test")
        val validator2 = cachingFactory.create("test")

        assertThat(validator1)
            .isNotNull()
            .isSameAs(validator2)
    }
}
