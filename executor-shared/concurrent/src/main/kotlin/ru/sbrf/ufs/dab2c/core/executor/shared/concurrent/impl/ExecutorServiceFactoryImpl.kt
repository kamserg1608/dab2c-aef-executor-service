package ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.api.ExecutorServiceFactory
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.model.ExecutorType
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.model.ExecutorType.DEFAULT
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.model.ExecutorType.FIXED_THREAD
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.model.ExecutorType.USE_VIRTUAL_THREADS
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.model.ExecutorType.VIRTUAL_THREAD_PER_TASK
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool

/**
 * Default implementation of [ExecutorServiceFactory].
 */
class ExecutorServiceFactoryImpl : ExecutorServiceFactory {

    override fun getExecutor(executorType: ExecutorType, poolSize: Int?): ExecutorService = when (executorType) {

        FIXED_THREAD -> Executors.newFixedThreadPool(
            poolSize ?: throw IllegalAccessError("Pool size cant be null")
        )

        VIRTUAL_THREAD_PER_TASK, USE_VIRTUAL_THREADS -> Executors.newVirtualThreadPerTaskExecutor()

        DEFAULT -> ForkJoinPool.commonPool()
    }
}
