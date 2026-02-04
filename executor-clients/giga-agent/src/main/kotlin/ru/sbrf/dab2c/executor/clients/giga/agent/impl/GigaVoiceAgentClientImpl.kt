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
import kotlin.collections.mapOf

private val logger = KotlinLogging.logger {}

private const val HTTP_OK = 200
private const val CLASS_NAME = "GigaVoiceAgentClient"
private const val SETTINGS_ENDPOINT = "/settings"
private const val FUNCTIONS_ENDPOINT = "/functions"

/**
 * Implementation of GigaVoice Agent API client using Ktor HTTP client.
 */
class GigaVoiceAgentClientImpl @Suppress("LongParameterList") constructor(
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
    private val baseUrl: String,
    private val settingsRequestBuilder: GigaVoiceSettingsRequestBuilder,
    private val functionCallRequestBuilder: GigaVoiceFunctionCallRequestBuilder,
    private val monitoringService: MonitoringServiceFactory,
    private val mapper: GigaVoiceSettingsMapper = GigaVoiceSettingsMapper
) : GigaVoiceAgentClient {

    // Кэш для счётчиков и таймеров — ключ: метрика + теги
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

        // Получаем или создаём таймер
        val timerKey = buildTimerKey(
            metric = ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
            platform = context.daPlatform,
            channel = context.daChannel,
            tags = mapOf(
                "destination_service" to "gigavoice-agent",
                "endpoint" to SETTINGS_ENDPOINT,
                "method" to "POST",
                "channel" to context.daChannel,
                "platform" to context.daPlatform
            )
        )
        val timer = timerCache.getOrPut(timerKey) {
            monitoringService.createTimer(
                ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
                context.daPlatform,
                context.daChannel,
                tagsMap = mapOf(
                    "destination_service" to "gigavoice-agent",
                    "endpoint" to SETTINGS_ENDPOINT,
                    "method" to "POST",
                    "channel" to context.daChannel,
                    "platform" to context.daPlatform
                )
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
                    setBody(request)
                }

                // Счётчик вызовов
                val counterKey = buildCounterKey(
                    metric = ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
                    platform = context.daPlatform,
                    channel = context.daChannel,
                    tags = mapOf(
                        "destination_service" to "gigavoice-agent",
                        "status_code" to response.status.value.toString(),
                        "endpoint" to SETTINGS_ENDPOINT,
                        "channel" to context.daChannel,
                        "platform" to context.daPlatform
                    )
                )
                val counter = counterCache.getOrPut(counterKey) {
                    monitoringService.createCounter(
                        ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
                        context.daPlatform,
                        context.daChannel,
                        tagsMap = mapOf(
                            "destination_service" to "gigavoice-agent",
                            "status_code" to response.status.value.toString(),
                            "endpoint" to SETTINGS_ENDPOINT,
                            "channel" to context.daChannel,
                            "platform" to context.daPlatform
                        )
                    )
                }
                counter.increment()

                response.body<GigaVoiceSettingsResponseSchema>()
            }
        } catch (e: Exception) {
            timer.record {}
            throw e
        }

        timer.record {} // Завершаем измерение времени

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

        // Получаем или создаём таймер
        val timerKey = buildTimerKey(
            metric = ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
            platform = context.daPlatform,
            channel = context.daChannel,
            tags = mapOf(
                "destination_service" to "ab-ivr",
                "endpoint" to FUNCTIONS_ENDPOINT,
                "method" to "POST",
                "function_name" to (functionCalling.functionCall.name),
                "channel" to context.daChannel,
                "platform" to context.daPlatform
            )
        )
        val timer = timerCache.getOrPut(timerKey) {
            monitoringService.createTimer(
                ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
                context.daPlatform,
                context.daChannel,
                tagsMap = mapOf(
                    "destination_service" to "ab-ivr",
                    "endpoint" to FUNCTIONS_ENDPOINT,
                    "method" to "POST",
                    "function_name" to (functionCalling.functionCall.name),
                    "channel" to context.daChannel,
                    "platform" to context.daPlatform
                )
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

                // Счётчик вызовов
                val counterKey = buildCounterKey(
                    metric = ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
                    platform = context.daPlatform,
                    channel = context.daChannel,
                    tags = mapOf(
                        "destination_service" to "ab-ivr",
                        "status_code" to response.status.value.toString(),
                        "endpoint" to FUNCTIONS_ENDPOINT,
                        "function_name" to (functionCalling.functionCall.name),
                        "channel" to context.daChannel,
                        "platform" to context.daPlatform
                    )
                )
                val counter = counterCache.getOrPut(counterKey) {
                    monitoringService.createCounter(
                        ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
                        context.daPlatform,
                        context.daChannel,
                        tagsMap = mapOf(
                            "destination_service" to "ab-ivr",
                            "status_code" to response.status.value.toString(),
                            "endpoint" to FUNCTIONS_ENDPOINT,
                            "function_name" to (functionCalling.functionCall.name),
                            "channel" to context.daChannel,
                            "platform" to context.daPlatform
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
     * Строит уникальный ключ для кэширования счётчика.
     */
    private fun buildCounterKey(
        metric: ClientMetric,
        platform: String,
        channel: String,
        tags: Map<String, String>
    ): String = buildString {
        append("$metric|$platform|$channel|")
        tags.toSortedMap().forEach { (k, v) -> append("$k=$v;") }
    }

    /**
     * Строит уникальный ключ для кэширования таймера.
     */
    private fun buildTimerKey(
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
}
