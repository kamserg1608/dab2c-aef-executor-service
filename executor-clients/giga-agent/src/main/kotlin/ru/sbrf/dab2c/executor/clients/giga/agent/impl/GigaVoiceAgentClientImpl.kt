package ru.sbrf.dab2c.executor.clients.giga.agent.impl

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.sbrf.dab2c.executor.clients.common.model.ClientMetric
import ru.sbrf.dab2c.executor.clients.common.util.buildFullUrl
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceFunctionCallRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceSettingsMapper
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceSettingsRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.model.ACLAgentAnalytics
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunctionsResponseSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceSettingsResponseSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsResult
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.library.monitoring.service.api.CounterMetric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MonitoringServiceFactory
import ru.sbrf.dab2c.executor.library.monitoring.service.api.RecordMetric
import ru.sbrf.dab2c.executor.logging.IntegrationLogger

private val logger = KotlinLogging.logger {}

private const val HTTP_OK = 200
private const val CLASS_NAME = "GigaVoiceAgentClient"
private const val SETTINGS_ENDPOINT = "/settings"
private const val FUNCTIONS_ENDPOINT = "/functions"

/**
 * Implementation of GigaVoice Agent API client using Ktor HTTP client.
 */
class GigaVoiceAgentClientImpl
@Suppress("LongParameterList")
constructor(
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
    private val baseUrl: String,
    private val settingsRequestBuilder: GigaVoiceSettingsRequestBuilder,
    private val functionCallRequestBuilder: GigaVoiceFunctionCallRequestBuilder,
    private val monitoringService: MonitoringServiceFactory,
    private val mapper: GigaVoiceSettingsMapper = GigaVoiceSettingsMapper
) : GigaVoiceAgentClient {

    private val counterCache = mutableMapOf<String, CounterMetric>()
    private val timerCache = mutableMapOf<String, RecordMetric>()

    @Suppress("LongMethod")
    override suspend fun getSettings(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        voiceSettings: VoiceSettings,
        daSessionInfo: DaSessionInfo,
        contextData: ContextData,
    ): SettingsResult {
        logger.debug { "Getting settings for session: ${context.ufsSession}" }

        val request = settingsRequestBuilder.build(
            context, agentConfiguration, voiceSettings, daSessionInfo, contextData
        )
        val requestJson = objectMapper.writeValueAsString(request)

        val channel = context.daChannel
        val platform = context.daPlatform

        val timerTags = mapOf(
            TAG_DESTINATION_SERVICE to GIGA_VOICE_AGENT,
            TAG_ENDPOINT to SETTINGS_ENDPOINT,
            TAG_METHOD to POST,
            TAG_CHANNEL to channel,
            TAG_PLATFORM to platform
        )

        val timerKey = buildCacheKey(
            metric = ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
            platform = context.daPlatform,
            channel = context.daChannel,
            tags = timerTags
        )
        val timer = timerCache.getOrPut(timerKey) {
            monitoringService.createTimer(
                ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
                context.daPlatform,
                context.daChannel,
                tagsMap = timerTags
            )
        }

        val apiResponse = try {
            IntegrationLogger.logHttpCallSuspend(
                destinationSystem = baseUrl,
                destinationService = SETTINGS_ENDPOINT,
                rqMessage = requestJson,
                className = CLASS_NAME,
                responseExtractor = { response: GigaVoiceSettingsResponseSchema ->
                    objectMapper.writeValueAsString(response) to HTTP_OK
                }
            ) {
                val response = httpClient.post(buildFullUrl(baseUrl, SETTINGS_ENDPOINT)) {
                    contentType(ContentType.Application.Json)
                    with(context) { applyHeaders() }
                    with(context) { applyCookies() }
                    setBody(request)
                }

                val counterTags = timerTags + (TAG_STATUS_CODE to response.status.value.toString())

                val counterKey = buildCacheKey(
                    metric = ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
                    platform = context.daPlatform,
                    channel = context.daChannel,
                    tags = counterTags
                )
                val counter = counterCache.getOrPut(counterKey) {
                    monitoringService.createCounter(
                        ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
                        context.daPlatform,
                        context.daChannel,
                        tagsMap = counterTags
                    )
                }
                counter.increment()

                val responseStr = response.body<String>()
                println("Response: $responseStr")
                response.body<GigaVoiceSettingsResponseSchema>()
            }
        } catch (e: Exception) {
            timer.record {}
            throw e
        }

        timer.record {}

        return SettingsResult(
            settings = mapper.toDomainSettings(apiResponse.settings),
            performers = mapper.toDomainPerformers(apiResponse.performers),
            analytics = apiResponse.agentAnalytics?.map { it.toDomain() }.orEmpty()
        )
    }

    @Suppress("LongMethod")
    override suspend fun executeFunctionCall(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCallingData,
        daSessionInfo: DaSessionInfo,
        contextData: ContextData
    ): FunctionCallResult {
        logger.debug { "Executing function call for session: ${context.ufsSession}" }

        val request = functionCallRequestBuilder.build(
            context, agentConfiguration, functionCalling, daSessionInfo, contextData
        )
        val requestJson = objectMapper.writeValueAsString(request)

        val channel = context.daChannel
        val platform = context.daPlatform
        val functionName = functionCalling.functionCall.name

        val timerTags = mapOf(
            TAG_DESTINATION_SERVICE to AB_IVR,
            TAG_ENDPOINT to FUNCTIONS_ENDPOINT,
            TAG_METHOD to POST,
            TAG_FUNCTION_NAME to functionName,
            TAG_CHANNEL to channel,
            TAG_PLATFORM to platform
        )

        val timerKey = buildCacheKey(
            metric = ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
            platform = context.daPlatform,
            channel = context.daChannel,
            tags = timerTags
        )
        val timer = timerCache.getOrPut(timerKey) {
            monitoringService.createTimer(
                ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
                context.daPlatform,
                context.daChannel,
                tagsMap = timerTags
            )
        }

        val apiResponse = try {
            IntegrationLogger.logHttpCallSuspend(
                destinationSystem = baseUrl,
                destinationService = FUNCTIONS_ENDPOINT,
                rqMessage = requestJson,
                className = CLASS_NAME,
                responseExtractor = { response: GigaVoiceFunctionsResponseSchema ->
                    objectMapper.writeValueAsString(response) to HTTP_OK
                }
            ) {
                val response = httpClient.post(buildFullUrl(baseUrl, FUNCTIONS_ENDPOINT)) {
                    contentType(ContentType.Application.Json)
                    with(context) { applyHeaders() }
                    setBody(request)
                }

                val counterTags = timerTags + (TAG_STATUS_CODE to response.status.value.toString())

                // Счётчик вызовов
                val counterKey = buildCacheKey(
                    metric = ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
                    platform = context.daPlatform,
                    channel = context.daChannel,
                    tags = counterTags
                )
                val counter = counterCache.getOrPut(counterKey) {
                    monitoringService.createCounter(
                        ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
                        context.daPlatform,
                        context.daChannel,
                        tagsMap = mapOf(
                            TAG_DESTINATION_SERVICE to AB_IVR,
                            TAG_STATUS_CODE to response.status.value.toString(),
                            TAG_ENDPOINT to FUNCTIONS_ENDPOINT,
                            TAG_FUNCTION_NAME to functionCalling.functionCall.name,
                            TAG_CHANNEL to context.daChannel,
                            TAG_PLATFORM to context.daPlatform
                        )
                    )
                }
                counter.increment()

                response.body<GigaVoiceFunctionsResponseSchema>()
            }
        } catch (e: Exception) {
            timer.record {}
            throw e
        }

        timer.record {}

        return FunctionCallResult(
            result = mapper.toDomainFunctionResult(apiResponse.functionResult),
            analytics = apiResponse.agentAnalytics?.map { it.toDomain() }.orEmpty()
        )
    }

    /**
     * Строит уникальный ключ для кэширования таймера.
     */
    private fun buildCacheKey(
        metric: ClientMetric,
        platform: String,
        channel: String,
        tags: Map<String, String>
    ): String = buildString {
        append("$metric|$platform|$channel|")
        tags.toSortedMap().forEach { (k, v) -> append("$k=$v;") }
    }

    private fun ACLAgentAnalytics.toDomain(): AgentAnalytics = AgentAnalytics(
        dataVersion = dataVersion,
        data = objectMapper.writeValueAsString(data)
    )

    /**
     * Tags for metrics.
     */
    companion object Tags {
        const val TAG_DESTINATION_SERVICE = "destination_service"
        const val TAG_ENDPOINT = "endpoint"
        const val TAG_METHOD = "method"
        const val TAG_STATUS_CODE = "status_code"
        const val TAG_FUNCTION_NAME = "function_name"
        const val TAG_CHANNEL = "channel"
        const val TAG_PLATFORM = "platform"
        const val POST = "post"
        const val GIGA_VOICE_AGENT = "gigavoice-agent"
        const val AB_IVR = "ab-ivr"
    }
}
