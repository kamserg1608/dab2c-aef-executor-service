package ru.sbrf.dab2c.executor.voice.service.api

import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogTurnExtra

/**
 * Publishes completed dialog turns (input-output pairs) for analytics.
 */
interface DialogTurnPublisher {

    /**
     * Publishes a completed dialog turn consisting of user input and assistant output.
     */
    suspend fun publishDialogTurn(
        inputText: String,
        outputText: String,
        assistantResponseTime: Long,
        extra: DialogTurnExtra? = null,
        totalTokens: Int? = null
    )
}
