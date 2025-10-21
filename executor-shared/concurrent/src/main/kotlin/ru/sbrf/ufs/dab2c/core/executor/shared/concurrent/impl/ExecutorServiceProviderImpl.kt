package ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.api.ExecutorServiceFactory
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.api.ExecutorServiceProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.config.properties.ExecutorTypeProperties
import java.util.concurrent.ExecutorService

/**
 * Default implementation of [ExecutorServiceProvider].
 */
class ExecutorServiceProviderImpl(
    executorTypeProperties: ExecutorTypeProperties,
    executorServiceFactory: ExecutorServiceFactory
) : ExecutorServiceProvider {

    private val executor: ExecutorService = executorServiceFactory.getExecutor(
        executorTypeProperties.type,
        executorTypeProperties.fixedThreadPoolSize
    )

    override fun getExecutorService(): ExecutorService = executor
}
