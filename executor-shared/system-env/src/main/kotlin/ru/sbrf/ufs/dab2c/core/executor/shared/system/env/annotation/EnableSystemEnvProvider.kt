package ru.sbrf.ufs.dab2c.core.executor.shared.system.env.annotation

import org.springframework.context.annotation.Import
import ru.sbrf.ufs.dab2c.core.executor.shared.system.env.SystemEnvProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.system.env.config.SystemEnvProviderConfiguration

/**
 * Enables [SystemEnvProvider].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(SystemEnvProviderConfiguration::class)
annotation class EnableSystemEnvProvider
