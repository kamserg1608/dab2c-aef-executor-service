package ru.sbrf.dab2c.executor.clients.giga.agent.stub

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AudioSettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunction
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunctionsRequestSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunctionsResponseSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceSettingsRequestSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceSettingsResponseSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Input
import ru.sbrf.dab2c.executor.clients.giga.agent.model.OutputOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Performers
import ru.sbrf.dab2c.executor.clients.giga.agent.model.PostProcessContextRequestSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.PostProcessContextResponseSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsOutput
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * Stub REST controller for GigaVoice Agent API endpoints.
 * Active only when STUB profile is enabled.
 */
@RestController
@Profile("STUB-CLIENTS")
class GigaVoiceAgentStubController {

    private val settingsCallCounter = AtomicLong(0)
    private val functionsCallCounter = AtomicLong(0)
    private val postProcessCallCounter = AtomicLong(0)

    /**
     * Returns stub settings response mirroring input settings.
     */
    @PostMapping("/settings")
    fun getSettings(
        @RequestBody request: GigaVoiceSettingsRequestSchema,
        @RequestHeader("UFS-SESSION", required = false) ufsSession: String?
    ): GigaVoiceSettingsResponseSchema {
        val callNumber = settingsCallCounter.incrementAndGet()
        logger.info { "STUB /settings called #$callNumber for session: $ufsSession" }

        return GigaVoiceSettingsResponseSchema(
            settings = buildSettingsOutput(request.settings),
            performers = createStubPerformers(),
            agentAnalytics = emptyList()
        )
    }

    /**
     * Returns stub function execution result.
     */
    @PostMapping("/functions")
    fun executeFunctionCall(
        @RequestBody request: GigaVoiceFunctionsRequestSchema,
        @RequestHeader("UFS-SESSION", required = false) ufsSession: String?
    ): GigaVoiceFunctionsResponseSchema {
        val callNumber = functionsCallCounter.incrementAndGet()
        val functionCall = request.functionCalling.functionCall

        logger.info { "STUB /functions called #$callNumber for session: $ufsSession, function: ${functionCall.name}" }

        return GigaVoiceFunctionsResponseSchema(
            functionResult = FunctionResult(
                content = buildStubResult(functionCall.name, functionCall.arguments, callNumber),
                functionName = functionCall.name
            ),
            agentAnalytics = emptyList()
        )
    }

    /**
     * Returns stub post-processing result with no analytics.
     */
    @PostMapping("/postprocess")
    fun postProcess(
        @RequestBody request: PostProcessContextRequestSchema,
        @RequestHeader("UFS-SESSION", required = false) ufsSession: String?
    ): PostProcessContextResponseSchema {
        val callNumber = postProcessCallCounter.incrementAndGet()
        logger.info {
            "STUB /postprocess called #$callNumber for session: $ufsSession, conversation: ${request.conversationId}"
        }

        return PostProcessContextResponseSchema(agentAnalytics = emptyList())
    }

    private fun buildSettingsOutput(input: SettingsInput): SettingsOutput {
        val inputAudio = input.audio.input
        return SettingsOutput(
            audio = AudioSettingsOutput(
                input = Input(audioEncoding = inputAudio?.audioEncoding, sampleRate = inputAudio?.sampleRate),
                output = OutputOutput(voice = "stub-voice", audioEncoding = inputAudio?.audioEncoding)
            ),
            voiceCallId = input.voiceCallId,
            disableVad = input.disableVad,
            enableTranscribeInput = input.enableTranscribeInput,
            flags = input.flags,
            mode = input.mode,
            firstSpeaker = input.firstSpeaker,
            enableDenoiser = input.enableDenoiser,
            enablePrefetch = input.enablePrefetch,
            enablePersonIdentity = input.enablePersonIdentity,
            enableWhisper = input.enableWhisper,
            enableEmotion = input.enableEmotion
        )
    }

    private fun createStubPerformers(): Performers = Performers(
        functions = listOf(
            GigaVoiceFunction(name = "stub_function", isBackendFunction = true),
            GigaVoiceFunction(name = "transfer_to_operator", isBackendFunction = false),
            GigaVoiceFunction(name = "end_dialog", isBackendFunction = false)
        )
    )

    private fun buildStubResult(functionName: String, arguments: String?, callNumber: Long): String {
        val args = arguments ?: "null"
        return """{"status":"success","function":"$functionName","arguments":$args,"call_number":$callNumber}"""
    }
}
