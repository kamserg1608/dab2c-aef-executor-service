package ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.api

import java.util.concurrent.ExecutorService

/**
 * An interface that provides an [ExecutorService] instance.
 */
interface ExecutorServiceProvider {

    /**
     * Provides [ExecutorService] instance.
     */
    fun getExecutorService(): ExecutorService
}
