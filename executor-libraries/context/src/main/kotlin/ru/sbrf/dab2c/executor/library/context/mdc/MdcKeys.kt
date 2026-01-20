package ru.sbrf.dab2c.executor.library.context.mdc

/**
 * Standard MDC keys provided by the context library.
 *
 * These keys are automatically registered with [RequestContext] on initialization.
 */
enum class MdcKeys(override val key: String) : MdcKey {
    /**
     * Unique identifier for tracing a request through the system.
     * Typically propagated from incoming gRPC metadata or HTTP headers.
     */
    REQUEST_ID("requestId")
}
