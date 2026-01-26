package ru.sbrf.dab2c.executor.clients.efs.adapter.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.mapper.AgentConfigurationMapper
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.AppSourceRequest
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseMapStringAgentConfig
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration

private val logger = KotlinLogging.logger {}

private const val DEFAULT_APP_SOURCE = "default"

/**
 * Implementation of Configurator API client using Ktor HTTP client.
 */
class ConfiguratorClientImpl(
    private val httpClient: HttpClient
) : ConfiguratorClient {

    private val mapper = AgentConfigurationMapper.INSTANCE

    override suspend fun getRestAgentConfig(agentName: String, cookie: String): AgentConfiguration {
        logger.debug { "Getting REST agent config for agent: $agentName" }

        return try {
            val response: BaseResponseMapStringAgentConfig = httpClient.post("/configurator/rest-agent") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Cookie, cookie)
                setBody(AppSourceRequest(appSource = DEFAULT_APP_SOURCE))
            }.body()

            val agentConfig = response.body?.get(agentName)
                ?: throw NoSuchElementException("Agent config not found for agent: $agentName")

            mapper.toDomain(agentConfig)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get REST agent config for agent: $agentName" }
            throw e
        }
    }
}
