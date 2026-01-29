package ru.sbrf.dab2c.executor.clients.giga.agent.impl

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
import ru.sbrf.dab2c.executor.logging.IntegrationLogger

private val logger = KotlinLogging.logger {}

private const val HTTP_OK = 200
private const val CLASS_NAME = "GigaVoiceAgentClient"
private const val SETTINGS_ENDPOINT = "/settings"
private const val FUNCTIONS_ENDPOINT = "/functions"

/**
 * Implementation of GigaVoice Agent API client using Ktor HTTP client.
 */
class GigaVoiceAgentClientImpl(
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
    private val baseUrl: String,
    private val settingsRequestBuilder: GigaVoiceSettingsRequestBuilder,
    private val functionCallRequestBuilder: GigaVoiceFunctionCallRequestBuilder,
    private val mapper: GigaVoiceSettingsMapper = GigaVoiceSettingsMapper
) : GigaVoiceAgentClient {

    override suspend fun getSettings(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        voiceSettings: VoiceSettings,
        daSessionInfo: DaSessionInfo,
        contextData: ContextData
    ): SettingsResult {
        logger.debug { "Getting settings for session: ${context.ufsSession}" }

        val request = settingsRequestBuilder.build(
            context, agentConfiguration, voiceSettings, daSessionInfo, contextData
        )
        val requestJson = objectMapper.writeValueAsString(request)

        val apiResponse = IntegrationLogger.logHttpCallSuspend(
            destinationSystem = baseUrl,
            destinationService = SETTINGS_ENDPOINT,
            rqMessage = requestJson,
            className = CLASS_NAME,
            responseExtractor = { response: GigaVoiceSettingsResponseSchema ->
                objectMapper.writeValueAsString(response) to HTTP_OK
            }
        ) {
            httpClient.post(buildFullUrl(baseUrl, SETTINGS_ENDPOINT)) {
                contentType(ContentType.Application.Json)
                with(context) { applyHeaders() }
                setBody(request)
            }.body<GigaVoiceSettingsResponseSchema>()
        }

        return SettingsResult(
            settings = mapper.toDomainSettings(apiResponse.settings),
            performers = mapper.toDomainPerformers(apiResponse.performers),
            analytics = apiResponse.agentAnalytics?.map { it.toDomain() }.orEmpty()
        )
    }

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

        val apiResponse = IntegrationLogger.logHttpCallSuspend(
            destinationSystem = baseUrl,
            destinationService = FUNCTIONS_ENDPOINT,
            rqMessage = requestJson,
            className = CLASS_NAME,
            responseExtractor = { response: GigaVoiceFunctionsResponseSchema ->
                objectMapper.writeValueAsString(response) to HTTP_OK
            }
        ) {
            httpClient.post(buildFullUrl(baseUrl, FUNCTIONS_ENDPOINT)) {
                contentType(ContentType.Application.Json)
                with(context) { applyHeaders() }
                setBody(request)
            }.body<GigaVoiceFunctionsResponseSchema>()
        }

        return FunctionCallResult(
            result = mapper.toDomainFunctionResult(apiResponse.functionResult),
            analytics = apiResponse.agentAnalytics?.map { it.toDomain() }.orEmpty()
        )
    }

    private fun ACLAgentAnalytics.toDomain(): AgentAnalytics = AgentAnalytics(
        dataVersion = dataVersion,
        data = objectMapper.writeValueAsString(data)
    )
}
