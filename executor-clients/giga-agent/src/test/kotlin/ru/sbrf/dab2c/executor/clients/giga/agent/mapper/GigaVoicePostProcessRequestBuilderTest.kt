package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.RequestBuilderFixtures.agentConfiguration
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.RequestBuilderFixtures.daSessionInfo
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.RequestBuilderFixtures.objectNode
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.RequestBuilderFixtures.requestContext
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Verifies the body of the post-processing request: identifiers, config, session data and context.
 */
class GigaVoicePostProcessRequestBuilderTest {

    private val builder = GigaVoicePostProcessRequestBuilder()

    @Test
    fun `should fill identifiers config and session data from the request context`() {
        val request = build(DialogContext(objectNode("""{"a":1,"b":{"c":"d"}}""")))

        assertThat(request.conversationId).isEqualTo(requestContext.conversationId)
        assertThat(request.eduId).isEqualTo(requestContext.eduId)
        assertThat(request.config.sessionConfig.channel).isEqualTo(requestContext.channel)
        assertThat(request.config.agentConfig.name).isEqualTo(agentConfiguration.name)
        assertThat(request.sessionInfo).isNotNull
        assertThat(request.userInfo).isNotNull
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
        daSessionInfo = daSessionInfo,
        contextData = contextData
    )
}
