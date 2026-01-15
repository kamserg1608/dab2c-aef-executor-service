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
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.SdsClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.mapper.SdsSectionMapper
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseListSdsSectionData
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseVoid
import ru.sbrf.dab2c.executor.domain.sds.SdsSection

private val logger = KotlinLogging.logger {}

/**
 * Implementation of SDS API client using Ktor HTTP client.
 */
class SdsClientImpl(
    private val httpClient: HttpClient
) : SdsClient {

    private val mapper = SdsSectionMapper.INSTANCE

    override suspend fun readData(sections: List<SdsSection>, cookie: String): List<SdsSection> {
        logger.debug { "Reading SDS data for ${sections.size} section(s)" }

        val requestBody = sections.map { mapper.toSectionInfo(it) }

        val response: BaseResponseListSdsSectionData = httpClient.post("/session/readData") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Cookie, cookie)
            setBody(requestBody)
        }.body()

        checkErrors(response.errors, "readData")

        return response.result?.map { mapper.toDomain(it) } ?: emptyList()
    }

    override suspend fun writeData(sections: List<SdsSection>, cookie: String) {
        logger.debug { "Writing SDS data for ${sections.size} section(s)" }

        val requestBody = sections.map { mapper.toSectionData(it) }

        val response: BaseResponseVoid = httpClient.post("/session/writeData") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Cookie, cookie)
            setBody(requestBody)
        }.body()

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
