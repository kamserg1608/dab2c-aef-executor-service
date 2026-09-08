package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

internal object RequestBuilderFixtures {

    val requestContext = GigaAgentRequestContext(
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

    val agentConfiguration = AgentConfiguration(
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

    val daSessionInfo = DaSessionInfo(
        meta = DaSessionMeta(
            sessionId = "test-session-id",
            userId = "test-user-id",
            ucpId = "test-ucp-id",
            ufsHost = "test-host"
        ),
        common = DaSessionCommon(channel = "test-channel"),
        userInfo = DaSessionUserInfo()
    )

    fun objectNode(json: String): ObjectNode = ObjectMappers.MAPPER.readTree(json) as ObjectNode
}
