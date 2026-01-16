package ru.sbrf.dab2c.executor.clients.giga.agent.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceFunctionCallRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceSettingsMapper
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceSettingsRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunctionsResponseSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceSettingsResponseSchema
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.domain.voice.FunctionResultData
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings

private val logger = KotlinLogging.logger {}

private const val UFS_SESSION_HEADER = "UFS-SESSION"
private const val UFS_TOKEN_HEADER = "UFS-TOKEN"

/**
 * Implementation of GigaVoice Agent API client using Ktor HTTP client.
 */
class GigaVoiceAgentClientImpl(
    private val httpClient: HttpClient,
    private val settingsRequestBuilder: GigaVoiceSettingsRequestBuilder,
    private val functionCallRequestBuilder: GigaVoiceFunctionCallRequestBuilder,
    private val mapper: GigaVoiceSettingsMapper = GigaVoiceSettingsMapper
) : GigaVoiceAgentClient {

    override suspend fun getSettings(
        ufsSession: String,
        ufsToken: String,
        agentConfiguration: AgentConfiguration,
        voiceSettings: VoiceSettings,
        channel: String,
        conversationId: String,
        eduId: String
    ): Pair<VoiceSettings, FunctionPerformers> {
        logger.debug { "Getting settings for session: $ufsSession" }

        val request = settingsRequestBuilder.build(agentConfiguration, voiceSettings, channel, conversationId, eduId)

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
        functionCalling: FunctionCallingData,
        agentConfiguration: AgentConfiguration,
        channel: String,
        conversationId: String,
        eduId: String
    ): FunctionResultData {
        logger.debug { "Executing function call for session: $ufsSession" }

        val request = functionCallRequestBuilder.build(
            agentConfiguration, functionCalling, channel, conversationId, eduId
        )

        val apiResponse: GigaVoiceFunctionsResponseSchema = try {
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

        return mapper.toDomainFunctionResult(apiResponse.functionResult)
    }
}
