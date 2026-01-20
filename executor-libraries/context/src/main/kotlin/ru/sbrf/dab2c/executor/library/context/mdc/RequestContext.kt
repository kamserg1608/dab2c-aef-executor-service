package ru.sbrf.dab2c.executor.library.context.mdc

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.context.ContextRegistry
import org.slf4j.MDC
import reactor.core.publisher.Hooks
import reactor.util.context.Context

private val logger = KotlinLogging.logger {}

/**
 * Central API for MDC context management with automatic propagation through
 * ThreadLocal, Reactor Context, and Coroutine Context.
 *
 * This object provides:
 * - Getter/setter/remover methods for MDC values
 * - Reactor Context modifiers for use with `contextWrite()`
 * - Snapshot/restore functionality for manual context propagation
 */
object RequestContext {

    init {
        logger.info { "Initializing MDC context propagation" }

        val registry = ContextRegistry.getInstance()
        MdcKeys.entries.forEach { mdcKey ->
            registry.registerThreadLocalAccessor(
                mdcKey.key,
                { MDC.get(mdcKey.key) },
                { value -> MDC.put(mdcKey.key, value) },
                { MDC.remove(mdcKey.key) }
            )
        }

        Hooks.enableAutomaticContextPropagation()

        logger.info { "MDC context propagation initialized with keys: ${MdcKeys.entries.map { it.key }}" }
    }

    /**
     * Explicitly triggers initialization. Call before Spring starts to ensure
     * context propagation is set up before any reactive code runs.
     */
    fun init() {
        // Object initialization happens in init block above.
        // This method just provides explicit entry point.
    }

    /**
     * Set an MDC value in the current ThreadLocal context.
     *
     * Note: This only sets the ThreadLocal MDC. For propagation through reactive
     * code, also use [withValues] with `contextWrite()` or use [MdcScope] functions.
     */
    operator fun set(key: MdcKey, value: String) {
        MDC.put(key.key, value)
    }

    /**
     * Get an MDC value from the current ThreadLocal context.
     *
     * @return The value or null if not set
     */
    operator fun get(key: MdcKey): String? = MDC.get(key.key)

    /**
     * Remove an MDC value from the current ThreadLocal context.
     */
    fun remove(key: MdcKey) {
        MDC.remove(key.key)
    }

    /**
     * Clear all MDC values from the current ThreadLocal context.
     */
    fun clear() {
        MDC.clear()
    }

    /**
     * Create a Reactor Context modifier that adds the specified values.
     *
     * @param pairs Key-value pairs to add to the context. Null values are ignored.
     * @return A function that modifies a Reactor Context
     */
    fun withValues(vararg pairs: Pair<MdcKey, String?>): (Context) -> Context = { ctx ->
        pairs.fold(ctx) { context, (key, value) ->
            if (value != null) context.put(key.key, value) else context
        }
    }

    /**
     * Create a Reactor Context modifier that captures the current MDC state.
     *
     * @return A function that adds current MDC values to a Reactor Context
     */
    fun asContextModifier(): (Context) -> Context = { ctx ->
        MdcKeys.entries.fold(ctx) { context, mdcKey ->
            val value = MDC.get(mdcKey.key)
            if (value != null) context.put(mdcKey.key, value) else context
        }
    }

    /**
     * Snapshot of MDC values for manual propagation.
     */
    data class Snapshot(internal val values: Map<String, String>)

    /**
     * Capture current MDC state as a snapshot.
     */
    fun capture(): Snapshot {
        val values = MdcKeys.entries.mapNotNull { mdcKey ->
            MDC.get(mdcKey.key)?.let { value -> mdcKey.key to value }
        }.toMap()
        return Snapshot(values)
    }

    /**
     * Restore MDC values from a snapshot.
     *
     * @param snapshot The snapshot to restore from
     */
    fun restore(snapshot: Snapshot) {
        snapshot.values.forEach { (key, value) ->
            if (MDC.get(key) == null) {
                MDC.put(key, value)
            }
        }
    }
}
