package ru.sbrf.dab2c.executor.voice.service.impl

import com.fasterxml.jackson.databind.node.ObjectNode
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.Parameter
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.HeadersElement
import ru.sbrf.dab2c.executor.library.context.SessionInfoElement
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.voice.model.VoiceSessionFeatureToggles
import ru.sbrf.dab2c.executor.voice.model.VoiceSessionFeatureTogglesElement
import kotlin.coroutines.CoroutineContext

internal object DialogContextTestFixtures {

    const val CONVERSATION_ID = "test-conversation-id"

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

    fun dialogContext(json: String): DialogContext =
        DialogContext(ObjectMappers.MAPPER.readTree(json) as ObjectNode)

    fun sessionContext(
        configuratorFunctionMatch: Boolean = false,
        postProcessingEnabled: Boolean = false
    ): CoroutineContext =
        HeadersElement(Headers(emptyMap())) +
            SessionInfoElement(daSessionInfo) +
            VoiceSessionFeatureTogglesElement(featureToggles(configuratorFunctionMatch, postProcessingEnabled))

    private fun featureToggles(
        configuratorFunctionMatch: Boolean,
        postProcessingEnabled: Boolean
    ) = VoiceSessionFeatureToggles(
        mapOf(
            VoiceSessionFeatureToggles.KAP_SEND_EXTRA to
                Parameter(VoiceSessionFeatureToggles.KAP_SEND_EXTRA, "false"),
            VoiceSessionFeatureToggles.CONFIGURATOR_FUNCTION_MATCH to
                Parameter(
                    VoiceSessionFeatureToggles.CONFIGURATOR_FUNCTION_MATCH,
                    configuratorFunctionMatch.toString()
                ),
            VoiceSessionFeatureToggles.POSTPROCESSING_ENABLED to
                Parameter(
                    VoiceSessionFeatureToggles.POSTPROCESSING_ENABLED,
                    postProcessingEnabled.toString()
                )
        )
    )
}
