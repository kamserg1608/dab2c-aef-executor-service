package ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.sbrf.dab2c.executor.clients.giga.agent.model.ExecuteFunctionCallRequest
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GetSettings200Response
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GetSettingsRequest
import ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.api.GigaVoiceAgentClient

private val logger = KotlinLogging.logger {}

/**
 * Implementation of GigaVoice Agent API client using Ktor HTTP client.
 */
class GigaVoiceAgentClientImpl(
    private val httpClient: HttpClient
) : GigaVoiceAgentClient {

    override suspend fun getSettings(
        ufsSession: String,
        ufsToken: String,
        request: GetSettingsRequest
    ): GetSettings200Response {
        logger.debug { "Getting settings for session: $ufsSession" }

        return try {
            httpClient.post("/v1/settings") {
                contentType(ContentType.Application.Json)
                header("UFS-SESSION", ufsSession)
                header("UFS-TOKEN", ufsToken)
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
        request: ExecuteFunctionCallRequest
    ): FunctionResult {
        logger.debug { "Executing function call for session: $ufsSession" }

        return try {
            httpClient.post("/v1/function-call") {
                contentType(ContentType.Application.Json)
                header("UFS-SESSION", ufsSession)
                header("UFS-TOKEN", ufsToken)
                setBody(request)
            }.body()
        } catch (e: Exception) {
            logger.error(e) { "Failed to execute function call for session: $ufsSession" }
            throw e
        }
    }
}
