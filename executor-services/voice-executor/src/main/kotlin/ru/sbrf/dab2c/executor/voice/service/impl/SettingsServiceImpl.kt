package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ClosedSendChannelException
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Context
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaVoiceResponse
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.voice.config.properties.VoiceExecutorConfigurationProperties
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.VoiceSession
import ru.sbrf.dab2c.executor.voice.service.api.AnalyticsPublisher
import ru.sbrf.dab2c.executor.voice.service.api.SettingsService
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.error as protoError

private const val SETTINGS_CALCULATION_ERROR_STATUS = 1501

/** Implementation of [SettingsService] that resolves voice settings via EFS and GigaAgent. */
class SettingsServiceImpl(
    private val session: VoiceSession,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient,
    private val configuratorClient: ConfiguratorClient,
    private val configProperties: VoiceExecutorConfigurationProperties,
    private val analyticsPublisher: AnalyticsPublisher
) : SettingsService {

    private val logger = KotlinLogging.logger {}

    override suspend fun initSettingsCalculation(settings: Settings) {
        val currentState = session.state.value
        check(currentState is ProcessingState.AwaitingSettings) {
            "Expected AwaitingSettings state, but was ${currentState::class.simpleName}"
        }

        calculateSettingsAsync(settings, currentState.contextData)
    }

    private suspend fun calculateSettingsAsync(
        settings: Settings,
        contextData: Context
    ) {
        logger.debug { "Calculating settings for session" }

        session.launch {
            try {
                val settingsData = fetchSettingsData(settings, contextData)
                session.callbackChannels.downstream.send(
                    gigaVoiceRequest { this.settings = settingsData.settings }
                )
                updateStateAndPublish(settingsData, contextData)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ClosedSendChannelException) {
                logger.debug(e) { "Channel closed, session ended before settings completed" }
            } catch (e: Exception) {
                logger.error { "Failed to calculate settings: ${e.message}" }
                session.callbackChannels.upstream.send(
                    gigaVoiceResponse {
                        error = protoError {
                            status = SETTINGS_CALCULATION_ERROR_STATUS
                            message = e.message ?: "Settings calculation failed"
                        }
                    }
                )
            }
        }
    }

    @Suppress("LongMethod")
    private suspend fun fetchSettingsData(
        settings: Settings,
        contextData: Context
    ): SettingsData {
        val headers = currentHeaders()
        val agentConfiguration = configuratorClient.getRestAgentConfig(configProperties.agentName)

        logger.debug { "Fetched agent configuration: ${agentConfiguration.name}" }

        val conversationId = settings.voiceCallId

        val settingsResult = gigaVoiceAgentClient.getSettings(
            conversationId = conversationId,
            agentConfiguration = agentConfiguration,
            voiceSettings = settings,
            contextData = contextData
        )

        val backendFuncs = settingsResult.performers.functions
            .filter { it.value.isBackendFunction }.keys
        val ivrFuncs = settingsResult.performers.functions
            .filterNot { it.value.isBackendFunction }.keys
        logger.debug {
            "Settings resolved: voiceCallId=${settings.voiceCallId}, " +
                "backendFunctions=$backendFuncs, ivrFunctions=$ivrFuncs"
        }

        return SettingsData(
            settings = settingsResult.settings,
            agentConfiguration = agentConfiguration,
            conversationId = conversationId,
            functionRegistry = settingsResult.performers,
            analytics = settingsResult.analytics,
            requestId = headers.getHeaderOrNull(RequestHeader.X_REQUEST_ID)
        )
    }

    private suspend fun updateStateAndPublish(settingsData: SettingsData, contextData: Context) {
        session.state.value = ProcessingState.Serving(
            contextData = contextData,
            agentConfiguration = settingsData.agentConfiguration,
            conversationId = settingsData.conversationId,
            functionRegistry = settingsData.functionRegistry
        )

        logger.info {
            "Session state -> Serving (conversationId=${settingsData.conversationId}, " +
                "functions=${settingsData.functionRegistry.functions.size})"
        }

        analyticsPublisher.publishAnalytics(settingsData.analytics, settingsData.requestId)
    }

    private data class SettingsData(
        val settings: Settings,
        val agentConfiguration: AgentConfiguration,
        val conversationId: String,
        val functionRegistry: FunctionPerformers,
        val analytics: List<AgentAnalytics>,
        val requestId: String?
    )
}
