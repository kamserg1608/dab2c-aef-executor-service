package ru.sbrf.dab2c.executor.clients.iag.mapper

import com.fasterxml.jackson.databind.node.ObjectNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCall
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCalling
import ru.sbrf.dab2c.executor.clients.iag.generated.model.IagMessageContent
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Verifies that the dialog context always reaches IAG as a single serialized context message.
 */
class IagFunctionRequestBuilderTest {

    private val builder = IagFunctionRequestBuilder()

    private val requestContext = GigaAgentRequestContext(
        ufsSession = "test-session",
        ufsToken = "test-token",
        channel = "test-channel",
        conversationId = "test-conversation-id",
        eduId = "test-edu-id",
        traceId = "test-trace-id",
        daRequestId = "test-request-id",
        daSessionId = "test-session-id",
        daChannel = "test-channel",
        daPlatform = "test-platform",
        daUcpId = "test-ucp-id"
    )

    private val agentConfiguration = AgentConfiguration(
        name = "test-agent",
        type = "voice",
        functionalSubsystemCi = "test-ci",
        description = "Test agent",
        entryPoints = emptyList(),
        ufsServiceAvailable = true,
        canAccessUserInfo = true,
        toolsMeta = emptyList(),
        neighboursAgentMeta = emptyList(),
        toggles = emptyMap()
    )

    private val daSessionInfo = DaSessionInfo(
        meta = DaSessionMeta(
            sessionId = "test-session-id",
            userId = "test-user-id",
            ucpId = "test-ucp-id",
            ufsHost = "test-host"
        ),
        common = DaSessionCommon(channel = "test-channel"),
        userInfo = DaSessionUserInfo()
    )

    @Test
    fun `should send empty context as an empty json object`() {
        val message = build(DialogContext(ObjectMappers.MAPPER.createObjectNode())).message.content.message

        assertThat(message).containsExactly(IagMessageContent(type = "context", value = "{}"))
    }

    @Test
    fun `should send populated context serialized`() {
        val node = ObjectMappers.MAPPER.readTree("""{"a":1}""") as ObjectNode

        val message = build(DialogContext(node)).message.content.message

        assertThat(message).containsExactly(IagMessageContent(type = "context", value = """{"a":1}"""))
    }

    private fun build(contextData: DialogContext) = builder.build(
        context = requestContext,
        agentConfiguration = agentConfiguration,
        functionCalling = functionCalling {
            functionCall = functionCall {
                name = "find_bank_office_iag"
                arguments = """{"city":"Moscow"}"""
            }
            timestamp = 1000L
        },
        daSessionInfo = daSessionInfo,
        contextData = contextData
    )
}
