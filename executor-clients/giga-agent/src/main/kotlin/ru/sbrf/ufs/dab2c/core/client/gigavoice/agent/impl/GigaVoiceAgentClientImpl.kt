package ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunctionsRequestSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunctionsResponseSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceSettingsRequestSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceSettingsResponseSchema
import ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.api.GigaVoiceAgentClient

private val logger = KotlinLogging.logger {}

private const val UFS_SESSION_HEADER = "UFS-SESSION"
private const val UFS_TOKEN_HEADER = "UFS-TOKEN"

/**
 * Implementation of GigaVoice Agent API client using Ktor HTTP client.
 */
class GigaVoiceAgentClientImpl(
    private val httpClient: HttpClient
) : GigaVoiceAgentClient {

    override suspend fun getSettings(
        ufsSession: String,
        ufsToken: String,
        request: GigaVoiceSettingsRequestSchema
    ): GigaVoiceSettingsResponseSchema {
        logger.debug { "Getting settings for session: $ufsSession" }

        return try {
            httpClient.post("/settings") {
                contentType(ContentType.Application.Json)
                header(UFS_SESSION_HEADER, ufsSession)
                header(UFS_TOKEN_HEADER, ufsToken)
                setBody(request)
            }.body()
        } catch (e: Exception) {
            logger.error(e) { "Failed to get settings for session: $ufsSession" }
            throw e
        }
    }

    override suspend fun executeFunctionCall(
        ufsSession: String,
        ufsToken: String,
        request: GigaVoiceFunctionsRequestSchema
    ): GigaVoiceFunctionsResponseSchema {
        logger.debug { "Executing function call for session: $ufsSession" }

        return try {
            httpClient.post("/functions") {
                contentType(ContentType.Application.Json)
                header(UFS_SESSION_HEADER, ufsSession)
                header(UFS_TOKEN_HEADER, ufsToken)
                setBody(request)
            }.body()
        } catch (e: Exception) {
            logger.error(e) { "Failed to execute function call for session: $ufsSession" }
            throw e
        }
    }
}
