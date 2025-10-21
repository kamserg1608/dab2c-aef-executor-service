package ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.model

/**
 * Enum representing different types of executors that can be used in a thread pool.
 */
enum class ExecutorType {
    /**
     * Default executor type.
     */
    DEFAULT,

    /**
     * Executor with fixed thread pool size.
     */
    FIXED_THREAD,

    /**
     * Executor with virtual threads.
     */
    VIRTUAL_THREAD_PER_TASK,

    /**
     * Virtual threads executor usage flag.
     */
    USE_VIRTUAL_THREADS
}
