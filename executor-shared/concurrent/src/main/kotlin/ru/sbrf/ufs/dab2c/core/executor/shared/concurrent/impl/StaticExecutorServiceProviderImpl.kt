package ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.api.ExecutorServiceProvider
import java.util.concurrent.ExecutorService

/**
 * Implementation of [ExecutorServiceProvider]. Provides a static instance of [ExecutorService]
 */
class StaticExecutorServiceProviderImpl(
    executorService: ExecutorService
) : ExecutorServiceProvider {

    private val executor = executorService

    override fun getExecutorService(): ExecutorService = executor
}
