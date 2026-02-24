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
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.FUNCTIONS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.SETTINGS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceFunctionCallRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceSettingsMapper
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceSettingsRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.model.ACLAgentAnalytics
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunctionsResponseSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceSettingsResponseSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsResult
import ru.sbrf.dab2c.executor.clients.giga.agent.util.GigaAgentContextBuilder
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.library.context.currentSessionInfo
import ru.sbrf.dab2c.executor.logging.IntegrationLogger

private val logger = KotlinLogging.logger {}

private const val HTTP_OK = 200
private const val CLASS_NAME = "GigaVoiceAgentClient"

/**
 * HTTP implementation of [GigaVoiceAgentClient] using Ktor.
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
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        voiceSettings: VoiceSettings,
        contextData: ContextData,
    ): SettingsResult {
        val context = GigaAgentContextBuilder.buildRequestContext(conversationId)
        val daSessionInfo = currentSessionInfo()
        logger.debug { "Getting settings for session: ${context.ufsSession}" }

        val request = settingsRequestBuilder.build(
            context, agentConfiguration, voiceSettings, daSessionInfo, contextData
        )

        val apiResponse = IntegrationLogger.logHttpCallSuspend(
            destinationSystem = baseUrl,
            destinationService = SETTINGS_ENDPOINT,
            rqMessage = objectMapper.writeValueAsString(request),
            className = CLASS_NAME,
            responseExtractor = { response: GigaVoiceSettingsResponseSchema ->
                objectMapper.writeValueAsString(response) to HTTP_OK
            }
        ) {
            httpClient.post(buildFullUrl(baseUrl, SETTINGS_ENDPOINT)) {
                contentType(ContentType.Application.Json)
                with(context) { applyHeaders() }
                with(context) { applyCookies() }
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
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCallingData,
        contextData: ContextData
    ): FunctionCallResult {
        val context = GigaAgentContextBuilder.buildRequestContext(conversationId)
        val daSessionInfo = currentSessionInfo()
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
                with(context) { applyCookies() }
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
