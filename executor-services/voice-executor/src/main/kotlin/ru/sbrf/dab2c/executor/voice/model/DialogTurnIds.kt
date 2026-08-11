package ru.sbrf.dab2c.executor.voice.model

import java.util.UUID

/**
 * Per-session holder for ids that scope to a single dialog turn.
 */
class DialogTurnIds {
    @Volatile var userMessageId: String = newId()
        private set

    @Volatile var assistantMessageId: String = newId()
        private set

    /** Generate fresh ids for the next dialog turn. */
    fun rotate() {
        userMessageId = newId()
        assistantMessageId = newId()
    }

    private fun newId(): String = UUID.randomUUID().toString()
}
