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
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.SdsClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.mapper.SdsSectionMapper
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseListSdsSectionData
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseVoid
import ru.sbrf.dab2c.executor.domain.session.SdsSection
import ru.sbrf.dab2c.executor.library.context.currentUfsCookie
import ru.sbrf.dab2c.executor.logging.IntegrationLogger

private val logger = KotlinLogging.logger {}

private const val HTTP_OK = 200
private const val CLASS_NAME = "SdsClient"
private const val READ_DATA_ENDPOINT = "/session/readData"
private const val WRITE_DATA_ENDPOINT = "/session/writeData"

/**
 * Implementation of SDS API client using Ktor HTTP client.
 */
class SdsClientImpl(
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
    private val baseUrl: String
) : SdsClient {

    private val mapper = SdsSectionMapper.INSTANCE

    override suspend fun readData(sections: List<SdsSection>): List<SdsSection> {
        val cookie = currentUfsCookie()
        logger.debug { "Reading SDS data for ${sections.size} section(s)" }

        val requestBody = sections.map { mapper.toSectionInfo(it) }
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
                setBody(requestBody)
            }

            response.body<BaseResponseListSdsSectionData>()
        }

        logger.debug { "SDS readData returned ${response.body?.size ?: 0} section(s)" }

        checkErrors(response.errors, "readData")

        return response.body?.map { mapper.toDomain(it) } ?: emptyList()
    }

    override suspend fun writeData(sections: List<SdsSection>) {
        val cookie = currentUfsCookie()
        logger.debug { "Writing SDS data for ${sections.size} section(s)" }

        val requestBody = sections.map { mapper.toSectionData(it) }
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
