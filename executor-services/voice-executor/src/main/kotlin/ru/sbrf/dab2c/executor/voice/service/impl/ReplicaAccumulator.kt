package ru.sbrf.dab2c.executor.voice.service.impl

import ru.sbrf.dab2c.executor.voice.session.observer.Replica

/** Mutable builder for a [Replica]. The start timestamp is sticky — only the first set wins. */
internal class ReplicaAccumulator(private val nowMs: () -> Long) {
    private val chunks = mutableListOf<String>()
    private var startedAtMs: Long? = null
    private var endedAtMs: Long? = null

    val isEmpty: Boolean get() = chunks.isEmpty()
    val hasStarted: Boolean get() = startedAtMs != null

    fun markStarted(atMs: Long) {
        if (startedAtMs == null) startedAtMs = atMs
    }

    fun appendText(text: String, atMs: Long) {
        markStarted(atMs)
        chunks += text
        endedAtMs = atMs
    }

    fun markEnded(atMs: Long) {
        endedAtMs = atMs
    }

    fun toReplica(): Replica = Replica(
        text = chunks.joinToString(""),
        startedAtMs = startedAtMs ?: nowMs(),
        endedAtMs = endedAtMs ?: nowMs(),
    )
}
