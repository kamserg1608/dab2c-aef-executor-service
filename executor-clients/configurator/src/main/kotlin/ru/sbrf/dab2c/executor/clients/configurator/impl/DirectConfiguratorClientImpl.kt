package ru.sbrf.dab2c.executor.clients.configurator.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.common.util.buildFullUrl
import ru.sbrf.dab2c.executor.clients.configurator.api.DirectConfiguratorClient
import ru.sbrf.dab2c.executor.clients.configurator.api.DirectConfiguratorClient.Companion.FUNCTION_LIST_ENDPOINT
import ru.sbrf.dab2c.executor.clients.configurator.configuration.DirectConfiguratorClientConfiguration.Companion.CONFIGURATOR_HTTP_CLIENT_BEAN_NAME
import ru.sbrf.dab2c.executor.clients.configurator.configuration.properties.DirectConfiguratorClientConfigurationProperties
import ru.sbrf.dab2c.executor.clients.configurator.mapper.FunctionListResponseMapper
import ru.sbrf.dab2c.executor.clients.configurator.model.BaseResponseFunctionListResponse
import ru.sbrf.dab2c.executor.domain.configuration.FunctionListResponse
import ru.sbrf.dab2c.executor.library.context.currentUfsCookie
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.logging.IntegrationLogger

private val logger = KotlinLogging.logger {}

private const val HTTP_OK = 200
private const val CLASS_NAME = "ConfiguratorFacade"

/**
 * Merged implementation of all Configurator API calls.
 */
@Service("directConfiguratorClientImpl")
class DirectConfiguratorClientImpl(
    @Qualifier(CONFIGURATOR_HTTP_CLIENT_BEAN_NAME)
    private val httpClient: HttpClient,
    properties: DirectConfiguratorClientConfigurationProperties
) : DirectConfiguratorClient {

    private val baseUrl = properties.baseUrl
    private val objectMapper = ObjectMappers.MAPPER
    private val functionListResponseMapper = FunctionListResponseMapper.INSTANCE

    override suspend fun fetchFunctionRegistry(
        agentName: String,
        modality: String
    ): FunctionListResponse {
        val request = mapOf(
            "agentName" to agentName,
            "modality" to modality
        )

        val response = fetchFunctionCallResponse(request)

        checkSuccess(response.success, "getFunctionCall")

        val mappedResponse = functionListResponseMapper.toDomain(response.body!!)

        logger.debug {
            "Function call response mapped successfully: $mappedResponse"
        }

        return mappedResponse
    }

    private suspend fun fetchFunctionCallResponse(
        request: Map<String, String>
    ): BaseResponseFunctionListResponse {
        val cookie = currentUfsCookie()
        val requestJson = objectMapper.writeValueAsString(request)

        val responseJson = IntegrationLogger.logHttpCallSuspend(
            destinationSystem = baseUrl,
            destinationService = FUNCTION_LIST_ENDPOINT,
            rqMessage = requestJson,
            className = CLASS_NAME,
            responseExtractor = { resp: String ->
                resp to HTTP_OK
            }
        ) {
            httpClient.post(buildFullUrl(baseUrl, FUNCTION_LIST_ENDPOINT)) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Cookie, cookie)
                applyTracingHeaders()
                setBody(request)
            }.body<String>()
        }

        return objectMapper.readValue(
            responseJson,
            BaseResponseFunctionListResponse::class.java
        )
    }

    private fun checkSuccess(
        success: Boolean?,
        operation: String
    ) {
        if (success == false) {
            error("Configurator $operation failed: success=false")
        }
    }
}
