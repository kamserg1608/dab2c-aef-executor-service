package ru.sbrf.dab2c.executor.clients.iag.impl

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.sbrf.dab2c.executor.clients.common.util.buildFullUrl
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.giga.agent.util.GigaAgentContextBuilder
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionResult
import ru.sbrf.dab2c.executor.clients.iag.api.IagFunctionClient
import ru.sbrf.dab2c.executor.clients.iag.api.IagFunctionClient.Companion.FUNCTION_CALL_ENDPOINT
import ru.sbrf.dab2c.executor.clients.iag.mapper.IagFunctionRequestBuilder
import ru.sbrf.dab2c.executor.clients.iag.model.IagAgentAnalytics
import ru.sbrf.dab2c.executor.clients.iag.model.IagFunctionResponse
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.library.context.currentSessionInfo
import ru.sbrf.dab2c.executor.logging.IntegrationLogger

private val logger = KotlinLogging.logger {}

private const val HTTP_OK = 200
private const val CLASS_NAME = "IagFunctionCallClient"

/**
 * HTTP implementation of [IagFunctionClient] using Ktor.
 */
class IagFunctionClientImpl(
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
    private val baseUrl: String,
    private val functionCallRequestBuilder: IagFunctionRequestBuilder
) : IagFunctionClient {

    @Suppress("LongMethod")
    override suspend fun executeFunctionCall(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCalling,
        contextData: DialogContext,
        endpoint: String?
    ): FunctionCallResult {
        val functionCallEndpoint = endpoint ?: FUNCTION_CALL_ENDPOINT
        val daSessionInfo = currentSessionInfo()
        val context = GigaAgentContextBuilder.buildRequestContext(conversationId, daSessionInfo, currentHeaders())
        logger.debug { "Executing IAG function call for session: ${context.ufsSession}" }

        val request = functionCallRequestBuilder.build(
            context, agentConfiguration, functionCalling, daSessionInfo, contextData
        )
        val requestJson = objectMapper.writeValueAsString(request)

        val response = IntegrationLogger.logHttpCallSuspend(
            destinationSystem = baseUrl,
            destinationService = functionCallEndpoint,
            rqMessage = requestJson,
            className = CLASS_NAME,
            responseExtractor = { resp: IagFunctionResponse ->
                objectMapper.writeValueAsString(resp) to HTTP_OK
            }
        ) {
            httpClient.post(buildFullUrl(baseUrl, functionCallEndpoint)) {
                contentType(ContentType.Application.Json)
                with(context) { applyHeaders() }
                with(context) { applyCookies() }
                setBody(request)
            }.body<IagFunctionResponse>()
        }

        checkError(response)

        val iagFunctionResult = response.content?.functionResult

        return FunctionCallResult(
            result = functionResult {
                content = serializeBody(iagFunctionResult?.content)
                functionName = iagFunctionResult?.name ?: functionCalling.functionCall.name
            },
            analytics = response.xAnalytics
                .flatMap { it.agentAnalytics }
                .map { it.toDomain() }
        )
    }

    private fun checkError(response: IagFunctionResponse) {
        response.xContext?.error?.let { error ->
            error(
                "IAG function call failed: " +
                    "type=${error.type}, " +
                    "label=${error.label}, " +
                    "description=${error.description}, " +
                    "payload=${serializeBody(error.payload)}"
            )
        }
    }

    private fun IagAgentAnalytics.toDomain(): AgentAnalytics = AgentAnalytics(
        dataVersion = dataVersion,
        data = objectMapper.writeValueAsString(data)
    )

    private fun serializeBody(body: JsonNode?): String =
        if (body == null || body.isNull) {
            "null"
        } else if (body.isTextual) {
            body.asText()
        } else {
            objectMapper.writeValueAsString(body)
        }
}
