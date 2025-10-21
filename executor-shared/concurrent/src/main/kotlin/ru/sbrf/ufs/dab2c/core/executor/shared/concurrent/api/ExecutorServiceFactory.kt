package ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.api

import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.model.ExecutorType
import java.util.concurrent.ExecutorService

/**
 * Factory, allows to obtain [ExecutorService] instance.
 */
interface ExecutorServiceFactory {

    /**
     * Factory method, allows to obtain [ExecutorService] instance via [ExecutorType].
     */
    fun getExecutor(executorType: ExecutorType, poolSize: Int?): ExecutorService
}
