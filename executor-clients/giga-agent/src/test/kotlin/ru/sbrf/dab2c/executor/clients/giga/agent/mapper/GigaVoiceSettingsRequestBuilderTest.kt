package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.RequestBuilderFixtures.agentConfiguration
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.RequestBuilderFixtures.daSessionInfo
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.RequestBuilderFixtures.objectNode
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.RequestBuilderFixtures.requestContext
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audioSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.settings
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Verifies that the dialog context lands in the settings request body as a plain map.
 */
class GigaVoiceSettingsRequestBuilderTest {

    private val builder = GigaVoiceSettingsRequestBuilder()

    private val voiceSettings = settings {
        voiceCallId = "call-123"
        audio = audioSettings { }
    }

    @Test
    fun `should convert populated context node into request context map`() {
        val request = build(DialogContext(objectNode("""{"a":1,"b":{"c":"d"}}""")))

        assertThat(request.context).isEqualTo(mapOf("a" to 1, "b" to mapOf("c" to "d")))
    }

    @Test
    fun `should convert empty context node into empty map`() {
        val request = build(DialogContext(ObjectMappers.MAPPER.createObjectNode()))

        assertThat(request.context).isEmpty()
    }

    private fun build(contextData: DialogContext) = builder.build(
        context = requestContext,
        agentConfiguration = agentConfiguration,
        voiceSettings = voiceSettings,
        daSessionInfo = daSessionInfo,
        contextData = contextData
    )
}
