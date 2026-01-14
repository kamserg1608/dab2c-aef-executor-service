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
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceSettingsResponseSchema
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.api.GigaVoiceAgentClient
import ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.mapper.GigaVoiceSettingsMapper
import ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.mapper.GigaVoiceSettingsRequestBuilder

private val logger = KotlinLogging.logger {}

private const val UFS_SESSION_HEADER = "UFS-SESSION"
private const val UFS_TOKEN_HEADER = "UFS-TOKEN"

/**
 * Implementation of GigaVoice Agent API client using Ktor HTTP client.
 */
class GigaVoiceAgentClientImpl(
    private val httpClient: HttpClient,
    private val requestBuilder: GigaVoiceSettingsRequestBuilder,
    private val mapper: GigaVoiceSettingsMapper = GigaVoiceSettingsMapper
) : GigaVoiceAgentClient {

    override suspend fun getSettings(
        ufsSession: String,
        ufsToken: String,
        agentConfiguration: AgentConfiguration,
        voiceSettings: VoiceSettings,
        channel: String
    ): Pair<VoiceSettings, FunctionPerformers> {
        logger.debug { "Getting settings for session: $ufsSession" }

        val request = requestBuilder.build(agentConfiguration, voiceSettings, channel)

        val apiResponse: GigaVoiceSettingsResponseSchema = try {
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

        return mapper.toDomainSettings(apiResponse.settings) to mapper.toDomainPerformers(apiResponse.performers)
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
