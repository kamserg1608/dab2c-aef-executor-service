package ru.sbrf.ufs.dab2c.core.executor.shared.system.env.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.ufs.dab2c.core.executor.shared.annotations.ProdProfileOnly
import ru.sbrf.ufs.dab2c.core.executor.shared.annotations.StubProfileOnly
import ru.sbrf.ufs.dab2c.core.executor.shared.system.env.DefaultSystemEnvProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.system.env.StubSystemEnvProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.system.env.SystemEnvProvider
import java.util.Optional

/**
 * [SystemEnvProvider] spring beans configuration.
 */
@Configuration
class SystemEnvProviderConfiguration {

    @Bean
    @ProdProfileOnly
    internal fun defaultSystemEnvProvider(): SystemEnvProvider = DefaultSystemEnvProvider()

    @Bean
    @StubProfileOnly
    internal fun stubSystemEnvProvider(
        @Qualifier(STUB_SYSTEM_ENV) optionalStubEnvironment: Optional<Map<String, String>>
    ): SystemEnvProvider = StubSystemEnvProvider(optionalStubEnvironment.orElseGet { emptyMap() })

    /**
     * Companion object.
     */
    companion object {
        /** Provide Map<String, String> bean with that @Qualifier in stub node to provide stub env. */
        const val STUB_SYSTEM_ENV = "STUB_SYSTEM_ENV"
    }
}
