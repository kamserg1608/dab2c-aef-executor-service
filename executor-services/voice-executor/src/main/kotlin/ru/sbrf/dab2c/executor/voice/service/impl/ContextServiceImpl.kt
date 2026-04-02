package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.VoiceSession
import ru.sbrf.dab2c.executor.voice.service.api.ContextService

/**
 * Default implementation of ContextService.
 */
class ContextServiceImpl(
    private val session: VoiceSession
) : ContextService {

    private val logger = KotlinLogging.logger {}

    override suspend fun processContext(contextData: ContextData) {
        logger.debug { "Processing context chunk: contentLength=${contextData.content.length}" }

        session.state.value = ProcessingState.AwaitingSettings(contextData)

        logger.info { "Session state -> AwaitingSettings" }
    }
}
