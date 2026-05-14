package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AnyExampleOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AudioSettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaChatSettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunction
import ru.sbrf.dab2c.executor.clients.giga.agent.model.InitialContextOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Input
import ru.sbrf.dab2c.executor.clients.giga.agent.model.OutputOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Params
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Performers
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.StubSoundsOutput
import ru.sbrf.dab2c.executor.library.testing.golden.assertMatchesGolden
import java.math.BigDecimal
import ru.sbrf.dab2c.executor.clients.giga.agent.model.DisableInterruption as ApiDisableInterruption
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FilterSettings as ApiFilterSettings
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FirstSpeaker as ApiFirstSpeaker
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCall as ApiFunctionCall
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionRanker as ApiFunctionRanker
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionRegistry as ApiFunctionRegistry
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionResult as ApiFunctionResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionSoundRule as ApiFunctionSoundRule
import ru.sbrf.dab2c.executor.clients.giga.agent.model.LockFunctionExecution as ApiLockFunctionExecution
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Message as ApiMessage
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Pair as ApiPair
import ru.sbrf.dab2c.executor.clients.giga.agent.model.RequestContentSettings as ApiRequestContentSettings
import ru.sbrf.dab2c.executor.clients.giga.agent.model.ResponseContentSettings as ApiResponseContentSettings
import ru.sbrf.dab2c.executor.clients.giga.agent.model.TriggerFunction as ApiTriggerFunction
import ru.sbrf.dab2c.executor.clients.giga.agent.model.TriggerGeneration as ApiTriggerGeneration

/**
 * Snapshot tests for [GigaVoiceApiToProtoMapper]. Each test feeds a fully-populated API
 * response model into a public entry point and asserts the entire serialized JSON output
 * against a golden file. Proto outputs serialize as proto3 JSON via [ProtobufModule]
 * registered on `ObjectMappers.MAPPER`.
 */
class GigaVoiceApiToProtoMapperTest {

    @Test
    fun `toProtoSettings maps a fully-populated SettingsOutput to proto Settings`() {
        val result = GigaVoiceApiToProtoMapper.toProtoSettings(fullSettingsOutput())

        assertMatchesGolden(result, "golden/GigaVoiceApiToProtoMapper/settings.json")
    }

    @Test
    fun `toProtoFunctionResult maps a fully-populated ApiFunctionResult to proto FunctionResult`() {
        val api = ApiFunctionResult(
            content = """{"balance":1000,"currency":"RUB"}""",
            functionName = "get_balance"
        )

        val result = GigaVoiceApiToProtoMapper.toProtoFunctionResult(api)

        assertMatchesGolden(result, "golden/GigaVoiceApiToProtoMapper/function-result.json")
    }

    @Test
    fun `toDomainPerformers maps Performers to domain FunctionPerformers`() {
        val api = Performers(
            functions = listOf(
                GigaVoiceFunction(name = "get_balance", isBackendFunction = true),
                GigaVoiceFunction(name = "transfer_money", isBackendFunction = false),
            )
        )

        val result = GigaVoiceApiToProtoMapper.toDomainPerformers(api)

        assertMatchesGolden(result, "golden/GigaVoiceApiToProtoMapper/performers.json")
    }

    @Suppress("LongMethod")
    private fun fullSettingsOutput(): SettingsOutput = SettingsOutput(
        voiceCallId = "call-uuid-1",
        audio = AudioSettingsOutput(
            input = Input(
                model = "asr-v2",
                audioEncoding = 1,
                sampleRate = 16_000,
                silencePhrases = listOf("silence-a", "silence-b"),
                silencePhrasesTimeout = "5s",
                silenceTimeout = "3s",
                stopPhrases = listOf("stop-it"),
                ignorePhrases = listOf("ignore-me"),
            ),
            output = OutputOutput(
                voice = "Nina",
                audioEncoding = 2,
                stubSounds = StubSoundsOutput(
                    sounds = listOf("intro-sound"),
                    triggerGeneration = ApiTriggerGeneration(
                        timeout = "2s",
                        enable = true,
                    ),
                    triggerFunction = ApiTriggerFunction(
                        enable = true,
                        mode = 1,
                        functionNames = listOf("get_balance"),
                        rules = listOf(
                            ApiFunctionSoundRule(
                                functionNames = listOf("transfer_money"),
                                sounds = listOf("sound-1"),
                            )
                        ),
                    ),
                ),
                speed = 3,
            ),
        ),
        gigachat = GigaChatSettingsOutput(
            model = "GigaChat-Pro",
            temperature = BigDecimal("0.7"),
            topP = BigDecimal("0.9"),
            repetitionPenalty = BigDecimal("1.1"),
            updateInterval = BigDecimal("0.5"),
            profanityCheck = true,
            filtersSettings = mapOf(
                "filter-key" to ApiFilterSettings(
                    requestContent = ApiRequestContentSettings(
                        neuro = true,
                        blacklist = false,
                        whitelist = true,
                    ),
                    responseContent = ApiResponseContentSettings(blacklist = true),
                )
            ),
            functions = listOf(
                FunctionOutput(
                    name = "get_balance",
                    description = "Returns user balance",
                    parameters = """{"type":"object"}""",
                    fewShotExamples = listOf(
                        AnyExampleOutput(
                            request = "What is my balance?",
                            params = Params(pairs = listOf(ApiPair(key = "user_id", value = "42"))),
                        )
                    ),
                    returnParameters = """{"type":"number"}""",
                )
            ),
            functionRegistry = ApiFunctionRegistry(
                profile = "default",
                labels = listOf("voice", "rus"),
                abFlags = """{"experiment":"on"}""",
            ),
            filterStubPhrases = listOf("заглушка"),
            currentTime = 1_730_000_000,
            functionRanker = ApiFunctionRanker(
                enabled = true,
                topN = 5,
                embedderModel = "embedder-v1",
                ignoredFunctions = listOf("noop"),
            ),
            preset = "preset-a",
        ),
        context = InitialContextOutput(
            messages = listOf(
                ApiMessage(
                    role = "user",
                    content = "Hi there",
                    functionCall = ApiFunctionCall(name = "ping", arguments = """{"x":1}"""),
                    functionName = "ping",
                    functionsStateId = "state-1",
                    attachments = listOf("att-1"),
                    inlineData = mapOf("key-1" to "value-1"),
                    functions = listOf(
                        FunctionInput(name = "ping", description = "Ping")
                    ),
                )
            )
        ),
        disableVad = true,
        enableTranscribeInput = true,
        flags = listOf("flag-a", "flag-b"),
        outputModalities = 2,
        mode = 1,
        firstSpeaker = ApiFirstSpeaker(type = "agent", lockFirstIn = true),
        enableDenoiser = true,
        enablePrefetch = true,
        enablePersonIdentity = true,
        enableWhisper = true,
        enableEmotion = true,
        enableTranscribeSilencePhrases = true,
        disableInterruption = ApiDisableInterruption(
            functions = listOf(
                ApiLockFunctionExecution(
                    name = "transfer",
                    onExecution = true,
                    afterResult = false,
                )
            )
        ),
    )
}
