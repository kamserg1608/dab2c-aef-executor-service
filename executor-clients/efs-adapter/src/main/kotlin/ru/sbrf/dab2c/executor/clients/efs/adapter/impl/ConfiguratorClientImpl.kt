package ru.sbrf.dab2c.executor.clients.efs.adapter.impl

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import ru.sbrf.dab2c.executor.clients.common.util.buildFullUrl
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.mapper.AgentConfigurationMapper
import ru.sbrf.dab2c.executor.clients.efs.adapter.mapper.ConfiguratorMapper
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.AppSourceRequest
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseMapStringAgentConfig
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseSessionConfig
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.library.context.currentUfsCookie
import ru.sbrf.dab2c.executor.logging.IntegrationLogger

private val logger = KotlinLogging.logger {}

private const val HTTP_OK = 200
private const val CLASS_NAME = "ConfiguratorClient"
private const val DEFAULT_APP_SOURCE = ""
private const val REST_AGENT_ENDPOINT = "/configurator/rest-agent"
private const val SESSION_ENDPOINT = "/configurator/session"

/**
 * Implementation of Configurator API client using Ktor HTTP client.
 */
class ConfiguratorClientImpl(
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
    private val baseUrl: String
) : ConfiguratorClient {

    private val mapper = AgentConfigurationMapper.INSTANCE
    private val configuratorMapper = ConfiguratorMapper.INSTANCE

    override suspend fun getRestAgentConfig(agentName: String): AgentConfiguration {
        val cookie = currentUfsCookie()
        logger.debug { "Getting REST agent config for agent: $agentName" }

        val request = AppSourceRequest(appSource = DEFAULT_APP_SOURCE)
        val requestJson = objectMapper.writeValueAsString(request)

        val response = IntegrationLogger.logHttpCallSuspend(
            destinationSystem = baseUrl,
            destinationService = REST_AGENT_ENDPOINT,
            rqMessage = requestJson,
            className = CLASS_NAME,
            responseExtractor = { resp: BaseResponseMapStringAgentConfig ->
                objectMapper.writeValueAsString(resp) to HTTP_OK
            }
        ) {
            httpClient.post(buildFullUrl(baseUrl, REST_AGENT_ENDPOINT)) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Cookie, cookie)
                setBody(request)
            }.body<BaseResponseMapStringAgentConfig>()
        }

        val agentConfig = response.body?.get(agentName)
            ?: throw NoSuchElementException("Agent config not found for agent: $agentName")

        return mapper.toDomain(agentConfig)
    }

    override suspend fun getDaSessionCommon(): DaSessionCommon {
        val cookie = currentUfsCookie()
        val request = AppSourceRequest(appSource = DEFAULT_APP_SOURCE)
        val requestJson = objectMapper.writeValueAsString(request)

        val response = IntegrationLogger.logHttpCallSuspend(
            destinationSystem = baseUrl,
            destinationService = SESSION_ENDPOINT,
            rqMessage = requestJson,
            className = CLASS_NAME,
            responseExtractor = { resp: BaseResponseSessionConfig ->
                objectMapper.writeValueAsString(resp) to HTTP_OK
            }
        ) {
            httpClient.post(buildFullUrl(baseUrl, SESSION_ENDPOINT)) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Cookie, cookie)
                setBody(request)
            }.body<BaseResponseSessionConfig>()
        }

        return configuratorMapper.toDomain(response.body!!)
    }
}
