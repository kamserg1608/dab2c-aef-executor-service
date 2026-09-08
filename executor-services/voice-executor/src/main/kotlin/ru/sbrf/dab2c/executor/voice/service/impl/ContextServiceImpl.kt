package ru.sbrf.dab2c.executor.voice.service.impl

import com.fasterxml.jackson.databind.node.ObjectNode
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.update
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Context
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
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

    override suspend fun processContext(contextData: Context) {
        logger.debug { "Processing context chunk: contentLength=${contextData.content.length}" }

        val parsed = ObjectMappers.MAPPER.readTree(contextData.content) as? ObjectNode
        checkNotNull(parsed) { "Context chunk does not carry a JSON object" }

        session.state.update { ProcessingState.AwaitingSettings(DialogContext(parsed)) }

        logger.info { "Session state -> AwaitingSettings" }
    }
}
