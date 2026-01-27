package ru.sbrf.dab2c.executor.voice.service.impl

import ru.sbrf.dab2c.executor.voice.service.api.DialogTurnPublisher

/**
 * No-op implementation for proxy mode where dialog publishing is not needed.
 */
object NoopDialogTurnPublisher : DialogTurnPublisher {

    override suspend fun publishDialogTurn(inputText: String, outputText: String, assistantResponseTime: Long) {
        // No-op: proxy mode does not publish dialogs
    }
}
