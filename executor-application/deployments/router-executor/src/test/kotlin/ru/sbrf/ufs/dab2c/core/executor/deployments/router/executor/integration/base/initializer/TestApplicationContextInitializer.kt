package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.base.initializer

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

@Suppress("StringLiteralDuplication")
class TestApplicationContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        System.setProperty("ru.sbrf.ufs.platform.envmode.dev", "true")
        System.setProperty("healthcheck.offline", "true")
    }
}
