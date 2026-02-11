package ru.sbrf.dab2c.executor.clients.giga.agent.monitoring

import ru.sbrf.dab2c.executor.clients.common.model.ClientMetric
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.FUNCTIONS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.SETTINGS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsResult
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MonitoringServiceFactory

/**
 * Monitoring decorator for [GigaVoiceAgentClient].
 * Wraps each call with timer and counter metrics.
 */
class MonitoringGigaVoiceAgentClientDecorator(
    private val delegate: GigaVoiceAgentClient,
    private val monitoringService: MonitoringServiceFactory
) : GigaVoiceAgentClient {

    override suspend fun getSettings(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        voiceSettings: VoiceSettings,
        daSessionInfo: DaSessionInfo,
        contextData: ContextData,
    ): SettingsResult {
        val timerTags = mapOf(
            TAG_DESTINATION_SERVICE to GIGA_VOICE_AGENT,
            TAG_ENDPOINT to SETTINGS_ENDPOINT,
            TAG_METHOD to POST,
            TAG_CHANNEL to context.daChannel,
            TAG_PLATFORM to context.daPlatform
        )

        val timer = monitoringService.createTimer(
            ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
            context.daPlatform,
            context.daChannel,
            tagsMap = timerTags
        )

        return timer.record {
            delegate.getSettings(context, agentConfiguration, voiceSettings, daSessionInfo, contextData).also {
                monitoringService.createCounter(
                    ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
                    context.daPlatform,
                    context.daChannel,
                    tagsMap = timerTags + (TAG_STATUS_CODE to STATUS_OK)
                ).increment()
            }
        }
    }

    override suspend fun executeFunctionCall(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCallingData,
        daSessionInfo: DaSessionInfo,
        contextData: ContextData
    ): FunctionCallResult {
        val timerTags = mapOf(
            TAG_DESTINATION_SERVICE to AB_IVR,
            TAG_ENDPOINT to FUNCTIONS_ENDPOINT,
            TAG_METHOD to POST,
            TAG_FUNCTION_NAME to functionCalling.functionCall.name,
            TAG_CHANNEL to context.daChannel,
            TAG_PLATFORM to context.daPlatform
        )

        val timer = monitoringService.createTimer(
            ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
            context.daPlatform,
            context.daChannel,
            tagsMap = timerTags
        )

        return timer.record {
            delegate.executeFunctionCall(
                context, agentConfiguration, functionCalling, daSessionInfo, contextData
            ).also {
                monitoringService.createCounter(
                    ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
                    context.daPlatform,
                    context.daChannel,
                    tagsMap = timerTags + (TAG_STATUS_CODE to STATUS_OK)
                ).increment()
            }
        }
    }

    /**
     * Metric tag constants.
     */
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
