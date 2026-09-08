package ru.sbrf.dab2c.executor.voice.service.impl

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.configurator.api.DirectConfiguratorClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.settings
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.voice.config.properties.VoiceExecutorConfigurationProperties
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.VoiceSession
import ru.sbrf.dab2c.executor.voice.service.api.AnalyticsPublisher
import ru.sbrf.dab2c.executor.voice.service.impl.DialogContextTestFixtures.CONVERSATION_ID
import ru.sbrf.dab2c.executor.voice.service.impl.DialogContextTestFixtures.agentConfiguration
import ru.sbrf.dab2c.executor.voice.service.impl.DialogContextTestFixtures.dialogContext
import ru.sbrf.dab2c.executor.voice.service.impl.DialogContextTestFixtures.sessionContext

/**
 * Verifies how the context returned by /settings lands in the serving state.
 */
class SettingsServiceImplTest {

    private val ivrContext = dialogContext("""{"ivr":true}""")

    private val gigaVoiceAgentClient: GigaVoiceAgentClient = mockk()
    private val analyticsPublisher: AnalyticsPublisher = mockk(relaxed = true)
    private val session = VoiceSession(
        state = MutableStateFlow(ProcessingState.AwaitingSettings(ivrContext))
    )

    private val service = SettingsServiceImpl(
        session = session,
        gigaVoiceAgentClient = gigaVoiceAgentClient,
        directConfiguratorClient = mockk<DirectConfiguratorClient>(),
        configuratorClient = mockk<ConfiguratorClient> {
            coEvery { getRestAgentConfig(any()) } returns agentConfiguration
        },
        configProperties = VoiceExecutorConfigurationProperties(),
        analyticsPublisher = analyticsPublisher
    )

    @Test
    fun `should keep the ivr context when the agent returns none`() = runTest {
        stubSettings(null)

        assertThat(servingContextAfterInit()).isEqualTo(ivrContext)
    }

    @Test
    fun `should replace the ivr context with the one returned by the agent`() = runTest {
        val agentContext = dialogContext("""{"a":1}""")
        stubSettings(agentContext)

        assertThat(servingContextAfterInit()).isEqualTo(agentContext)
    }

    private fun stubSettings(context: DialogContext?) {
        coEvery {
            gigaVoiceAgentClient.getSettings(any(), any(), any(), any())
        } returns SettingsResult(
            settings = settings { voiceCallId = CONVERSATION_ID },
            performers = FunctionPerformers(emptyMap()),
            context = context
        )
    }

    private suspend fun servingContextAfterInit(): DialogContext {
        withContext(sessionContext()) {
            service.initSettingsCalculation(settings { voiceCallId = CONVERSATION_ID })
        }
        session.callbackChannels.downstream.receive()

        return (session.state.value as ProcessingState.Serving).contextData
    }
}
