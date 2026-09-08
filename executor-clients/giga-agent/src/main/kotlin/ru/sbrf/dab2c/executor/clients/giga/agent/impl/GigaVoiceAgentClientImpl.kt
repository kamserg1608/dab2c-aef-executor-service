package ru.sbrf.dab2c.executor.clients.giga.agent.impl

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.sbrf.dab2c.executor.clients.common.util.buildFullUrl
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.FUNCTIONS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.POSTPROCESS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.SETTINGS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.configuration.properties.GigaAgentPostProcessProperties
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceApiToProtoMapper
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceFunctionCallRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoicePostProcessRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceSettingsRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.model.ACLAgentAnalytics
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunctionsResponseSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceSettingsResponseSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.PostProcessContextResponseSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.PostProcessResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsResult
import ru.sbrf.dab2c.executor.clients.giga.agent.util.GigaAgentContextBuilder
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.library.context.currentSessionInfo
import ru.sbrf.dab2c.executor.logging.IntegrationLogger

private val logger = KotlinLogging.logger {}

private const val HTTP_OK = 200
private const val CLASS_NAME = "GigaVoiceAgentClient"

/**
 * HTTP implementation of [GigaVoiceAgentClient] using Ktor.
 */
@Suppress("LongParameterList")
class GigaVoiceAgentClientImpl(
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
    private val baseUrl: String,
    private val settingsRequestBuilder: GigaVoiceSettingsRequestBuilder,
    private val functionCallRequestBuilder: GigaVoiceFunctionCallRequestBuilder,
    private val postProcessRequestBuilder: GigaVoicePostProcessRequestBuilder,
    private val postProcessProperties: GigaAgentPostProcessProperties,
) : GigaVoiceAgentClient {

    @Suppress("LongMethod")
    override suspend fun getSettings(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        voiceSettings: Settings,
        contextData: DialogContext,
    ): SettingsResult {
        val daSessionInfo = currentSessionInfo()
        val context = GigaAgentContextBuilder.buildRequestContext(conversationId, daSessionInfo, currentHeaders())
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
            settings = GigaVoiceApiToProtoMapper.toProtoSettings(apiResponse.settings),
            performers = GigaVoiceApiToProtoMapper.toDomainPerformers(apiResponse.performers),
            analytics = apiResponse.agentAnalytics?.map { it.toDomain() }.orEmpty(),
            context = apiResponse.context.toDialogContext()
        )
    }

    @Suppress("LongMethod")
    override suspend fun executeFunctionCall(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCalling,
        contextData: DialogContext
    ): FunctionCallResult {
        val daSessionInfo = currentSessionInfo()
        val context = GigaAgentContextBuilder.buildRequestContext(conversationId, daSessionInfo, currentHeaders())
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
            result = GigaVoiceApiToProtoMapper.toProtoFunctionResult(apiResponse.functionResult),
            analytics = apiResponse.agentAnalytics?.map { it.toDomain() }.orEmpty(),
            context = apiResponse.context.toDialogContext()
        )
    }

    @Suppress("LongMethod")
    override suspend fun postProcess(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        contextData: DialogContext
    ): PostProcessResult {
        val daSessionInfo = currentSessionInfo()
        val context = GigaAgentContextBuilder.buildRequestContext(conversationId, daSessionInfo, currentHeaders())
        logger.debug { "Post-processing conversation for session: ${context.ufsSession}" }

        val request = postProcessRequestBuilder.build(context, agentConfiguration, daSessionInfo, contextData)

        val apiResponse = IntegrationLogger.logHttpCallSuspend(
            destinationSystem = baseUrl,
            destinationService = POSTPROCESS_ENDPOINT,
            rqMessage = objectMapper.writeValueAsString(request),
            className = CLASS_NAME,
            responseExtractor = { response: PostProcessContextResponseSchema ->
                objectMapper.writeValueAsString(response) to HTTP_OK
            }
        ) {
            httpClient.post(buildFullUrl(baseUrl, POSTPROCESS_ENDPOINT)) {
                timeout {
                    requestTimeoutMillis = postProcessProperties.timeout
                    socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                }
                contentType(ContentType.Application.Json)
                with(context) { applyHeaders() }
                with(context) { applyCookies() }
                setBody(request)
            }.body<PostProcessContextResponseSchema>()
        }

        return PostProcessResult(analytics = apiResponse.agentAnalytics?.map { it.toDomain() }.orEmpty())
    }

    private fun Map<String, Any>?.toDialogContext(): DialogContext? =
        this?.let { DialogContext(objectMapper.valueToTree(it)) }

    private fun ACLAgentAnalytics.toDomain(): AgentAnalytics = AgentAnalytics(
        dataVersion = dataVersion,
        data = objectMapper.writeValueAsString(data)
    )
}
