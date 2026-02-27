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
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.common.util.buildFullUrl
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.AUDIT_EVENT_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.PERSON_INFO_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.READ_DATA_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.REST_AGENT_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.SESSION_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade.Companion.WRITE_DATA_ENDPOINT
import ru.sbrf.dab2c.executor.clients.efs.adapter.configuration.EfsAdapterClientConfiguration.Companion.EFS_ADAPTER_HTTP_CLIENT_BEAN_NAME
import ru.sbrf.dab2c.executor.clients.efs.adapter.configuration.properties.EfsAdapterClientConfigurationProperties
import ru.sbrf.dab2c.executor.clients.efs.adapter.mapper.AgentConfigurationMapper
import ru.sbrf.dab2c.executor.clients.efs.adapter.mapper.ConfiguratorMapper
import ru.sbrf.dab2c.executor.clients.efs.adapter.mapper.PersonInfoMapper
import ru.sbrf.dab2c.executor.clients.efs.adapter.mapper.SdsSectionMapper
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.AppSourceRequest
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.AuditEventServiceEvent
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseListSdsSectionData
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseMapStringAgentConfig
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseProfile
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseSessionConfig
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseVoid
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.domain.session.SdsSection
import ru.sbrf.dab2c.executor.library.audit.model.AuditEvent
import ru.sbrf.dab2c.executor.library.context.currentUfsCookie
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.logging.IntegrationLogger

private val logger = KotlinLogging.logger {}

private const val HTTP_OK = 200
private const val DEFAULT_APP_SOURCE = ""
private const val CLASS_NAME = "EfsFacade"

/**
 * Merged implementation of all EFS Adapter API calls.
 */
@Service("efsFacadeImpl")
class EfsFacadeImpl(
    @Qualifier(EFS_ADAPTER_HTTP_CLIENT_BEAN_NAME) private val httpClient: HttpClient,
    properties: EfsAdapterClientConfigurationProperties
) : EfsFacade {

    private val baseUrl = properties.baseUrl
    private val objectMapper = ObjectMappers.MAPPER
    private val agentConfigMapper = AgentConfigurationMapper.INSTANCE
    private val configuratorMapper = ConfiguratorMapper.INSTANCE
    private val personInfoMapper = PersonInfoMapper.INSTANCE
    private val sdsSectionMapper = SdsSectionMapper.INSTANCE

    @Suppress("LongMethod")
    override suspend fun sendEvent(event: AuditEvent) {
        val cookie = currentUfsCookie()
        logger.debug { "Sending audit event: ${event.event} params: ${event.params}" }

        val request = AuditEventServiceEvent(
            event = event.event,
            success = event.success,
            params = event.params.ifEmpty { null }
        )

        val response = IntegrationLogger.logHttpCallSuspend(
            destinationSystem = baseUrl,
            destinationService = AUDIT_EVENT_ENDPOINT,
            rqMessage = objectMapper.writeValueAsString(request),
            className = CLASS_NAME,
            responseExtractor = { resp: BaseResponseVoid ->
                objectMapper.writeValueAsString(resp) to HTTP_OK
            }
        ) {
            httpClient.post(buildFullUrl(baseUrl, AUDIT_EVENT_ENDPOINT)) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Cookie, cookie)
                applyTracingHeaders()
                setBody(request)
            }.body<BaseResponseVoid>()
        }

        val errors = response.errors
        if (!errors.isNullOrEmpty()) {
            val errorMessages = errors.joinToString { it.message ?: it.code ?: "Unknown error" }
            logger.warn { "Audit event '${event.event}' completed with errors: $errorMessages" }
        }
    }

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
                applyTracingHeaders()
                setBody(request)
            }.body<BaseResponseMapStringAgentConfig>()
        }

        val agentConfig = response.body?.get(agentName)
            ?: throw NoSuchElementException("Agent config not found for agent: $agentName")

        return agentConfigMapper.toDomain(agentConfig)
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
                applyTracingHeaders()
                setBody(request)
            }.body<BaseResponseSessionConfig>()
        }

        return configuratorMapper.toDomain(response.body!!)
    }

    override suspend fun getPersonInfo(): DaSessionUserInfo {
        val cookie = currentUfsCookie()
        val request = "[\"PERSON\"]"
        val requestJson = objectMapper.writeValueAsString(request)

        val response = IntegrationLogger.logHttpCallSuspend(
            destinationSystem = baseUrl,
            destinationService = PERSON_INFO_ENDPOINT,
            rqMessage = requestJson,
            className = CLASS_NAME,
            responseExtractor = { resp: BaseResponseProfile ->
                objectMapper.writeValueAsString(resp) to HTTP_OK
            }
        ) {
            httpClient.post(buildFullUrl(baseUrl, PERSON_INFO_ENDPOINT)) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Cookie, cookie)
                applyTracingHeaders()
                setBody(request)
            }.body<BaseResponseProfile>()
        }

        val person = response.body?.person
        val additionalInfo = person?.additionalInfo

        return personInfoMapper.toDomain(person, additionalInfo)
    }

    override suspend fun readData(sections: List<SdsSection>): List<SdsSection> {
        val cookie = currentUfsCookie()
        logger.debug { "Reading SDS data for ${sections.size} section(s)" }

        val requestBody = sections.map { sdsSectionMapper.toSectionInfo(it) }
        val requestJson = objectMapper.writeValueAsString(requestBody)

        val response = IntegrationLogger.logHttpCallSuspend(
            destinationSystem = baseUrl,
            destinationService = READ_DATA_ENDPOINT,
            rqMessage = requestJson,
            className = CLASS_NAME,
            responseExtractor = { resp: BaseResponseListSdsSectionData ->
                objectMapper.writeValueAsString(resp) to HTTP_OK
            }
        ) {
            val response = httpClient.post(buildFullUrl(baseUrl, READ_DATA_ENDPOINT)) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Cookie, cookie)
                applyTracingHeaders()
                setBody(requestBody)
            }

            response.body<BaseResponseListSdsSectionData>()
        }

        logger.debug { "SDS readData returned ${response.body?.size ?: 0} section(s)" }

        checkErrors(response.errors, "readData")

        return response.body?.map { sdsSectionMapper.toDomain(it) } ?: emptyList()
    }

    override suspend fun writeData(sections: List<SdsSection>) {
        val cookie = currentUfsCookie()
        logger.debug { "Writing SDS data for ${sections.size} section(s)" }

        val requestBody = sections.map { sdsSectionMapper.toSectionData(it) }
        val requestJson = objectMapper.writeValueAsString(requestBody)

        val response = IntegrationLogger.logHttpCallSuspend(
            destinationSystem = baseUrl,
            destinationService = WRITE_DATA_ENDPOINT,
            rqMessage = requestJson,
            className = CLASS_NAME,
            responseExtractor = { resp: BaseResponseVoid ->
                objectMapper.writeValueAsString(resp) to HTTP_OK
            }
        ) {
            httpClient.post(buildFullUrl(baseUrl, WRITE_DATA_ENDPOINT)) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Cookie, cookie)
                applyTracingHeaders()
                setBody(requestBody)
            }.body<BaseResponseVoid>()
        }

        checkErrors(response.errors, "writeData")
    }

    private fun checkErrors(
        errors: List<ru.sbrf.dab2c.executor.clients.efs.adapter.model.Error>?,
        operation: String
    ) {
        if (!errors.isNullOrEmpty()) {
            val errorMessages = errors.joinToString("; ") { "${it.code}: ${it.message}" }
            error("SDS $operation failed: $errorMessages")
        }
    }
}
