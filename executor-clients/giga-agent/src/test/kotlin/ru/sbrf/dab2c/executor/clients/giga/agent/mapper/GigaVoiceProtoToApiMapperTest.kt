package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import com.google.protobuf.duration
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.anyExample
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audioSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.disableInterruption
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.filterSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.firstSpeaker
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.function
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCall
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionRanker
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionRegistry
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionSoundRule
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaChatSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.initialContext
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.input
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.lockFunctionExecution
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.message
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.output
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.pair
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.params
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.requestContentSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.responseContentSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.settings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.stubSounds
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.triggerFunction
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.triggerGeneration
import ru.sbrf.dab2c.executor.library.testing.golden.assertMatchesGolden
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Input as ProtoInput
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Output as ProtoOutput
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings as ProtoSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.TriggerFunction as ProtoTriggerFunction

/**
 * Snapshot tests for [GigaVoiceProtoToApiMapper]. Each test feeds a fully-populated proto
 * input into a public entry point and asserts the entire serialized JSON output against a
 * golden file. Field coverage is achieved via the fixture, not via per-field assertions.
 */
class GigaVoiceProtoToApiMapperTest {

    @Test
    fun `toApiSettingsInput maps a fully-populated proto Settings to API SettingsInput`() {
        val result = GigaVoiceProtoToApiMapper.toApiSettingsInput(fullProtoSettings())

        assertMatchesGolden(result, "golden/GigaVoiceProtoToApiMapper/settings-input.json")
    }

    @Test
    fun `toApiFunctionCalling maps a fully-populated proto FunctionCalling to API FunctionCalling`() {
        val proto = functionCalling {
            functionCall = functionCall {
                name = "transfer_money"
                arguments = """{"amount":100,"currency":"RUB"}"""
            }
            timestamp = 1_730_000_000L
        }

        val result = GigaVoiceProtoToApiMapper.toApiFunctionCalling(proto)

        assertMatchesGolden(result, "golden/GigaVoiceProtoToApiMapper/function-calling.json")
    }

    @Suppress("LongMethod")
    private fun fullProtoSettings(): ProtoSettings = settings {
        voiceCallId = "call-uuid-1"
        audio = audioSettings {
            input = input {
                model = "asr-v2"
                audioEncoding = ProtoInput.AudioEncoding.PCM_S16LE
                sampleRate = 16_000
                silencePhrases.addAll(listOf("silence-a", "silence-b"))
                silencePhrasesTimeout = duration { seconds = 5 }
                silenceTimeout = duration { seconds = 3 }
                stopPhrases.addAll(listOf("stop-it"))
                ignorePhrases.addAll(listOf("ignore-me"))
            }
            output = output {
                voice = "Nina"
                audioEncoding = ProtoOutput.AudioEncoding.OPUS
                stubSounds = stubSounds {
                    triggerGeneration = triggerGeneration {
                        timeout = duration { seconds = 2 }
                        enable = true
                    }
                    triggerFunction = triggerFunction {
                        enable = true
                        mode = ProtoTriggerFunction.Mode.WHITELIST
                        functionNames.addAll(listOf("get_balance"))
                        rules.add(
                            functionSoundRule {
                                functionNames.addAll(listOf("transfer_money"))
                                sounds.addAll(listOf("sound-1"))
                            }
                        )
                    }
                    sounds.addAll(listOf("intro-sound"))
                }
                speed = ProtoOutput.Speed.MEDIUM
            }
        }
        gigachat = gigaChatSettings {
            model = "GigaChat-Pro"
            temperature = 0.7f
            topP = 0.9f
            repetitionPenalty = 1.1f
            updateInterval = 0.5f
            profanityCheck = true
            filtersSettings.put(
                "filter-key",
                filterSettings {
                    requestContent = requestContentSettings {
                        neuro = true
                        blacklist = false
                        whitelist = true
                    }
                    responseContent = responseContentSettings {
                        blacklist = true
                    }
                }
            )
            functions.add(
                function {
                    name = "get_balance"
                    description = "Returns user balance"
                    parameters = """{"type":"object"}"""
                    fewShotExamples.add(
                        anyExample {
                            request = "What is my balance?"
                            params = params {
                                pairs.add(
                                    pair {
                                        key = "user_id"
                                        value = "42"
                                    }
                                )
                            }
                        }
                    )
                    returnParameters = """{"type":"number"}"""
                }
            )
            functionRegistry = functionRegistry {
                profile = "default"
                labels.addAll(listOf("voice", "rus"))
                abFlags = """{"experiment":"on"}"""
            }
            filterStubPhrases.addAll(listOf("заглушка"))
            currentTime = 1_730_000_000
            functionRanker = functionRanker {
                enabled = true
                topN = 5
                embedderModel = "embedder-v1"
                ignoredFunctions.addAll(listOf("noop"))
            }
            preset = "preset-a"
        }
        context = initialContext {
            messages.add(
                message {
                    role = "user"
                    content = "Hi there"
                    functionCall = functionCall {
                        name = "ping"
                        arguments = """{"x":1}"""
                    }
                    functionName = "ping"
                    functionsStateId = "state-1"
                    attachments.addAll(listOf("att-1"))
                    inlineData.put("key-1", "value-1")
                    functions.add(
                        function {
                            name = "ping"
                            description = "Ping"
                        }
                    )
                }
            )
        }
        disableVad = true
        enableTranscribeInput = true
        flags.addAll(listOf("flag-a", "flag-b"))
        outputModalities = ProtoSettings.OutputModalities.AUDIO_TEXT
        mode = ProtoSettings.Mode.RECOGNIZE_GIGACHAT_SYNTHESIS
        firstSpeaker = firstSpeaker {
            type = "agent"
            lockFirstIn = true
        }
        enableDenoiser = true
        enablePrefetch = true
        enablePersonIdentity = true
        enableWhisper = true
        enableEmotion = true
        enableTranscribeSilencePhrases = true
        disableInterruption = disableInterruption {
            functions.add(
                lockFunctionExecution {
                    name = "transfer"
                    onExecution = true
                    afterResult = false
                }
            )
        }
    }
}
