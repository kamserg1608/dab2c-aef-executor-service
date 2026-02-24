package ru.sbrf.dab2c.executor.clients.giga.agent.monitoring

import ru.sbrf.dab2c.executor.clients.common.model.ClientMetric
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.FUNCTIONS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.SETTINGS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsResult
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MonitoringServiceFactory

/**
 * Decorator that records HTTP integration metrics for GigaVoice agent calls.
 */
class MonitoringGigaVoiceAgentClientDecorator(
    private val delegate: GigaVoiceAgentClient,
    private val monitoringService: MonitoringServiceFactory
) : GigaVoiceAgentClient {

    override suspend fun getSettings(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        voiceSettings: VoiceSettings,
        contextData: ContextData,
    ): SettingsResult {
        val (platform, channel) = currentPlatformAndChannel()

        val timerTags = mapOf(
            TAG_DESTINATION_SERVICE to GIGA_VOICE_AGENT,
            TAG_ENDPOINT to SETTINGS_ENDPOINT,
            TAG_METHOD to POST,
            TAG_CHANNEL to channel,
            TAG_PLATFORM to platform
        )

        val timer = monitoringService.createTimer(
            ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
            platform,
            channel,
            tagsMap = timerTags
        )

        return timer.record {
            delegate.getSettings(conversationId, agentConfiguration, voiceSettings, contextData).also {
                monitoringService.createCounter(
                    ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
                    platform,
                    channel,
                    tagsMap = timerTags + (TAG_STATUS_CODE to STATUS_OK)
                ).increment()
            }
        }
    }

    override suspend fun executeFunctionCall(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCallingData,
        contextData: ContextData
    ): FunctionCallResult {
        val (platform, channel) = currentPlatformAndChannel()

        val timerTags = mapOf(
            TAG_DESTINATION_SERVICE to AB_IVR,
            TAG_ENDPOINT to FUNCTIONS_ENDPOINT,
            TAG_METHOD to POST,
            TAG_FUNCTION_NAME to functionCalling.functionCall.name,
            TAG_CHANNEL to channel,
            TAG_PLATFORM to platform
        )

        val timer = monitoringService.createTimer(
            ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
            platform,
            channel,
            tagsMap = timerTags
        )

        return timer.record {
            delegate.executeFunctionCall(
                conversationId, agentConfiguration, functionCalling, contextData
            ).also {
                monitoringService.createCounter(
                    ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
                    platform,
                    channel,
                    tagsMap = timerTags + (TAG_STATUS_CODE to STATUS_OK)
                ).increment()
            }
        }
    }

    private suspend fun currentPlatformAndChannel(): Pair<String, String> {
        val headers = currentHeaders()
        return headers.getHeader(RequestHeader.PLATFORM) to headers.getHeader(RequestHeader.CHANNEL)
    }

    /** Metric tag constants. */
    companion object {
        private const val TAG_DESTINATION_SERVICE = "destination_service"
        private const val TAG_ENDPOINT = "endpoint"
        private const val TAG_METHOD = "method"
        private const val TAG_STATUS_CODE = "status_code"
        private const val TAG_FUNCTION_NAME = "function_name"
        private const val TAG_CHANNEL = "channel"
        private const val TAG_PLATFORM = "platform"
        private const val POST = "post"
        private const val GIGA_VOICE_AGENT = "gigavoice-agent"
        private const val AB_IVR = "ab-ivr"
        private const val STATUS_OK = "200"
    }
}
