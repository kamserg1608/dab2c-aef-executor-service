package ru.sbrf.dab2c.executor.voice.service.impl

import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.context
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.VoiceSession

/**
 * Verifies that the context chunk is the single parsing point and fails fast on anything but a JSON object.
 */
class ContextServiceImplTest {

    private val session = VoiceSession()
    private val service = ContextServiceImpl(session)

    @Test
    fun `should move to AwaitingSettings with parsed context on a json object`() = runTest {
        service.processContext(context { content = """{"a":1}""" })

        val expected = DialogContext(ObjectMappers.MAPPER.readTree("""{"a":1}""") as ObjectNode)
        assertThat(session.state.value).isEqualTo(ProcessingState.AwaitingSettings(expected))
    }

    @ParameterizedTest
    @ValueSource(strings = ["[]", "1", "not-a-json", ""])
    fun `should fail and keep AwaitingContext when chunk is not a json object`(content: String) = runTest {
        assertThrows<Exception> { service.processContext(context { this.content = content }) }

        assertThat(session.state.value).isEqualTo(ProcessingState.AwaitingContext)
    }
}
