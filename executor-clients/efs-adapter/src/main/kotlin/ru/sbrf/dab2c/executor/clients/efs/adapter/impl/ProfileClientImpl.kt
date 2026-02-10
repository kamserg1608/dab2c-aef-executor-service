package ru.sbrf.dab2c.executor.clients.efs.adapter.impl

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import ru.sbrf.dab2c.executor.clients.common.util.buildFullUrl
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ProfileClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.mapper.PersonInfoMapper
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseProfile
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.logging.IntegrationLogger

private const val HTTP_OK = 200
private const val CLASS_NAME = "ProfileClient"
private const val GET_PERSON_INFO = "/getPersonInfoByRegionKind"

/**
 * Implementation of Profile API client using Ktor HTTP client.
 */
class ProfileClientImpl(
    private val httpClient: HttpClient,
    private val objectMapper: ObjectMapper,
    private val baseUrl: String
) : ProfileClient {

    private val personInfoMapper = PersonInfoMapper.INSTANCE

    override suspend fun getPersonInfo(cookie: String): DaSessionUserInfo {

        val request = "[\"PERSON\"]"
        val requestJson = objectMapper.writeValueAsString(request)

        val response = IntegrationLogger.logHttpCallSuspend(
            destinationSystem = baseUrl,
            destinationService = GET_PERSON_INFO,
            rqMessage = requestJson,
            className = CLASS_NAME,
            responseExtractor = { resp: BaseResponseProfile ->
                objectMapper.writeValueAsString(resp) to HTTP_OK
            }
        ) {
            httpClient.post(buildFullUrl(baseUrl, GET_PERSON_INFO)) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Cookie, cookie)
                setBody(request)
            }.body<BaseResponseProfile>()
        }

        return personInfoMapper.toDomain(response.body?.person!!)
    }
}
