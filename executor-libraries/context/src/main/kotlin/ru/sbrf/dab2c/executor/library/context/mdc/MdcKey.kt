package ru.sbrf.dab2c.executor.library.context.mdc

/**
 * Interface for MDC keys that can be propagated through reactive/coroutine contexts.
 * Standard keys are defined in [MdcKeys].
 */
interface MdcKey {
    val key: String
}
