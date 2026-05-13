package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import com.google.protobuf.duration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AudioSettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaChatSettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.OutputOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.StubSoundsOutput
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Output
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audioSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.disableInterruption
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionRanker
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionSoundRule
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaChatSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.lockFunctionExecution
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.message
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.output
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.settings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.stubSounds
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.triggerFunction
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.triggerGeneration
import ru.sbrf.dab2c.executor.clients.giga.agent.model.DisableInterruption as ApiDisableInterruption
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionRanker as ApiFunctionRanker
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionSoundRule as ApiFunctionSoundRule
import ru.sbrf.dab2c.executor.clients.giga.agent.model.LockFunctionExecution as ApiLockFunctionExecution
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Message as ApiMessage
import ru.sbrf.dab2c.executor.clients.giga.agent.model.TriggerFunction as ApiTriggerFunction
import ru.sbrf.dab2c.executor.clients.giga.agent.model.TriggerGeneration as ApiTriggerGeneration
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.function as protoFunctionDsl

/**
 * Tests for [GigaVoiceSettingsMapper] focusing on boolean flag and flags list mappings.
 */
class GigaVoiceSettingsMapperTest {

    @Nested
    inner class ToApiSettingsInput {

        @Test
        fun `should map all boolean flags and flags list when populated`() {
            val proto = settings {
                voiceCallId = "test-call-id"
                audio = audioSettings { }
                disableVad = true
                enableTranscribeInput = true
                flags.addAll(listOf("flag1", "flag2"))
                enableDenoiser = true
                enablePrefetch = true
                enablePersonIdentity = true
                enableWhisper = true
                enableEmotion = true
                enableTranscribeSilencePhrases = true
            }

            val result = GigaVoiceSettingsMapper.toApiSettingsInput(proto)

            assertThat(result.disableVad).isTrue()
            assertThat(result.enableTranscribeInput).isTrue()
            assertThat(result.flags).containsExactly("flag1", "flag2")
            assertThat(result.enableDenoiser).isTrue()
            assertThat(result.enablePrefetch).isTrue()
            assertThat(result.enablePersonIdentity).isTrue()
            assertThat(result.enableWhisper).isTrue()
            assertThat(result.enableEmotion).isTrue()
            assertThat(result.enableTranscribeSilencePhrases).isTrue()
        }

        @Test
        fun `should map default proto values correctly`() {
            val proto = settings {
                voiceCallId = "test-call-id"
                audio = audioSettings { }
            }

            val result = GigaVoiceSettingsMapper.toApiSettingsInput(proto)

            assertThat(result.disableVad).isFalse()
            assertThat(result.enableTranscribeInput).isFalse()
            assertThat(result.flags).isNull()
            assertThat(result.enableDenoiser).isFalse()
            assertThat(result.enablePrefetch).isFalse()
            assertThat(result.enablePersonIdentity).isFalse()
            assertThat(result.enableWhisper).isFalse()
            assertThat(result.enableEmotion).isFalse()
            assertThat(result.enableTranscribeSilencePhrases).isFalse()
        }
    }

    @Nested
    inner class ToProtoSettings {

        @Test
        fun `should map all boolean flags and flags list when populated`() {
            val api = SettingsOutput(
                voiceCallId = "test-call-id",
                audio = AudioSettingsOutput(),
                disableVad = true,
                enableTranscribeInput = true,
                flags = listOf("flag1", "flag2"),
                enableDenoiser = true,
                enablePrefetch = true,
                enablePersonIdentity = true,
                enableWhisper = true,
                enableEmotion = true,
                enableTranscribeSilencePhrases = true
            )

            val result = GigaVoiceSettingsMapper.toProtoSettings(api)

            assertThat(result.disableVad).isTrue()
            assertThat(result.enableTranscribeInput).isTrue()
            assertThat(result.flagsList).containsExactly("flag1", "flag2")
            assertThat(result.enableDenoiser).isTrue()
            assertThat(result.enablePrefetch).isTrue()
            assertThat(result.enablePersonIdentity).isTrue()
            assertThat(result.enableWhisper).isTrue()
            assertThat(result.enableEmotion).isTrue()
            assertThat(result.enableTranscribeSilencePhrases).isTrue()
        }

        @Test
        fun `should use defaults when API fields are null`() {
            val api = SettingsOutput(
                voiceCallId = "test-call-id",
                audio = AudioSettingsOutput()
            )

            val result = GigaVoiceSettingsMapper.toProtoSettings(api)

            assertThat(result.disableVad).isFalse()
            assertThat(result.enableTranscribeInput).isFalse()
            assertThat(result.flagsList).isEmpty()
            assertThat(result.enableDenoiser).isFalse()
            assertThat(result.enablePrefetch).isFalse()
            assertThat(result.enablePersonIdentity).isFalse()
            assertThat(result.enableWhisper).isFalse()
            assertThat(result.enableEmotion).isFalse()
            assertThat(result.enableTranscribeSilencePhrases).isFalse()
        }
    }

    @Nested
    inner class RoundTrip {

        @Test
        fun `should preserve all fields through proto to API to proto conversion`() {
            val original = settings {
                voiceCallId = "round-trip-id"
                audio = audioSettings { output = output { speed = Output.Speed.MEDIUM } }
                gigachat = gigaChatSettings {
                    preset = "preset"
                    functionRanker = functionRanker {
                        enabled = true
                        embedderModel = "embed-model"
                        ignoredFunctions.add("function1")
                    }
                }
                disableVad = true
                enableTranscribeInput = true
                flags.addAll(listOf("alpha", "beta"))
                enableDenoiser = true
                enablePrefetch = false
                enablePersonIdentity = true
                enableWhisper = false
                enableEmotion = true
                enableTranscribeSilencePhrases = true
                disableInterruption = disableInterruption {
                    functions.add(
                        lockFunctionExecution {
                            name = "lock-function"
                            onExecution = true
                            afterResult = false
                        }
                    )
                }
            }

            val api = GigaVoiceSettingsMapper.toApiSettingsInput(original)
            val apiOutput = SettingsOutput(
                voiceCallId = api.voiceCallId,
                audio = AudioSettingsOutput(output = OutputOutput(speed = 3)),
                gigachat = GigaChatSettingsOutput(
                    preset = "preset",
                    functionRanker = ApiFunctionRanker(
                        enabled = true,
                        embedderModel = "embed-model",
                        ignoredFunctions = listOf("function1"),
                    )
                ),
                disableVad = api.disableVad,
                enableTranscribeInput = api.enableTranscribeInput,
                flags = api.flags,
                enableDenoiser = api.enableDenoiser,
                enablePrefetch = api.enablePrefetch,
                enablePersonIdentity = api.enablePersonIdentity,
                enableWhisper = api.enableWhisper,
                enableEmotion = api.enableEmotion,
                enableTranscribeSilencePhrases = api.enableTranscribeSilencePhrases,
                disableInterruption = ApiDisableInterruption(
                    functions = listOf(
                        ApiLockFunctionExecution(
                            name = "lock-function",
                            onExecution = true,
                            afterResult = false
                        )
                    )
                )
            )
            val result = GigaVoiceSettingsMapper.toProtoSettings(apiOutput)

            assertThat(result.disableVad).isEqualTo(original.disableVad)
            assertThat(result.enableTranscribeInput).isEqualTo(original.enableTranscribeInput)
            assertThat(result.flagsList).isEqualTo(original.flagsList)
            assertThat(result.enableDenoiser).isEqualTo(original.enableDenoiser)
            assertThat(result.enablePrefetch).isEqualTo(original.enablePrefetch)
            assertThat(result.enablePersonIdentity).isEqualTo(original.enablePersonIdentity)
            assertThat(result.enableWhisper).isEqualTo(original.enableWhisper)
            assertThat(result.enableEmotion).isEqualTo(original.enableEmotion)
            assertThat(result.enableTranscribeSilencePhrases).isEqualTo(original.enableTranscribeSilencePhrases)
            assertThat(result.audio.output.speed).isEqualTo(original.audio.output.speed)
            assertThat(result.gigachat.preset).isEqualTo(original.gigachat.preset)

            val ranker = result.gigachat.functionRanker
            val originalRanker = original.gigachat.functionRanker
            assertThat(ranker.enabled).isEqualTo(originalRanker.enabled)
            assertThat(ranker.embedderModel).isEqualTo(originalRanker.embedderModel)
            assertThat(ranker.ignoredFunctionsList).isEqualTo(originalRanker.ignoredFunctionsList)
        }
    }

    @Nested
    inner class StubSoundsMapping {

        @Test
        fun `should map proto StubSounds with triggerGeneration to API`() {
            val proto = stubSounds {
                triggerGeneration = triggerGeneration {
                    timeout = duration { seconds = 30L }
                    enable = true
                }
                triggerFunction = triggerFunction {
                    enable = true
                    mode = ru.sbrf.dab2c.executor.clients.gigavoice.proto.TriggerFunction.Mode.WHITELIST
                    functionNames.add("func1")
                    rules.add(
                        functionSoundRule {
                            functionNames.add("function1")
                            sounds.add("sound1")
                        }
                    )
                }
                sounds.addAll(listOf("sound1", "sound2"))
            }

            val result = GigaVoiceSettingsMapper.toApiStubSoundsInput(proto)

            assertThat(result.triggerGeneration!!.timeout).isEqualTo("30s")
            assertThat(result.triggerGeneration!!.enable).isTrue()
            assertThat(result.triggerFunction!!.enable).isTrue()
            assertThat(result.sounds).containsExactly("sound1", "sound2")
            assertThat(result.triggerFunction).isNotNull
            assertThat(result.triggerFunction!!.rules).hasSize(1)
            assertThat(result.triggerFunction!!.rules!![0].functionNames).containsExactly("function1")
            assertThat(result.triggerFunction!!.rules!![0].sounds).containsExactly("sound1")
        }

        @Test
        fun `should use defaults when proto triggerGeneration is unset`() {
            val proto = stubSounds { sounds.add("sound1") }

            val result = GigaVoiceSettingsMapper.toApiStubSoundsInput(proto)

            assertThat(result.triggerGeneration).isNull()
        }

        @Test
        fun `should use defaults when proto triggerFunction is unset`() {
            val proto = stubSounds { sounds.add("sound1") }

            val result = GigaVoiceSettingsMapper.toApiStubSoundsInput(proto)

            assertThat(result.triggerFunction).isNull()
        }

        @Test
        fun `should map API StubSoundsOutput with triggerGeneration to proto`() {
            val api = StubSoundsOutput(
                triggerGeneration = ApiTriggerGeneration(timeout = "15s", enable = true),
                triggerFunction = ApiTriggerFunction(
                    enable = true,
                    mode = 1,
                    functionNames = listOf("func1"),
                    rules = listOf(
                        ApiFunctionSoundRule(
                            functionNames = listOf("function1"),
                            sounds = listOf("sound1"),
                        )
                    )
                ),
                sounds = listOf("sound1")
            )

            val result = GigaVoiceSettingsMapper.toProtoStubSounds(api)

            assertThat(result.hasTriggerGeneration()).isTrue()
            assertThat(result.triggerGeneration.timeout.seconds).isEqualTo(15L)
            assertThat(result.triggerGeneration.enable).isTrue()
            assertThat(result.hasTriggerFunction()).isTrue()
            assertThat(result.triggerFunction.enable).isTrue()
            assertThat(result.soundsList).containsExactly("sound1")
            assertThat(result.triggerFunction.rulesList).hasSize(1)
            assertThat(result.triggerFunction.rulesList[0].functionNamesList).containsExactly("function1")
            assertThat(result.triggerFunction.rulesList[0].soundsList).containsExactly("sound1")
        }

        @Test
        fun `should leave triggerGeneration unset when API value is null`() {
            val api = StubSoundsOutput(
                triggerFunction = ApiTriggerFunction(
                    enable = false,
                    mode = 0,
                    functionNames = emptyList()
                ),
                sounds = emptyList()
            )

            val result = GigaVoiceSettingsMapper.toProtoStubSounds(api)

            assertThat(result.hasTriggerGeneration()).isFalse()
        }
    }

    @Nested
    inner class MessageMapping {

        @Test
        fun `should map proto message with functions to API`() {
            val proto = message {
                role = "assistant"
                content = "hello"
                functions.add(
                    protoFunctionDsl {
                        name = "func1"
                        description = "desc1"
                    }
                )
            }

            val result = GigaVoiceSettingsMapper.toApiMessage(proto)

            assertThat(result.functions).hasSize(1)
            assertThat(result.functions!![0].name).isEqualTo("func1")
            assertThat(result.functions!![0].description).isEqualTo("desc1")
        }

        @Test
        fun `should map proto message with empty functions to null in API`() {
            val proto = message {
                role = "user"
                content = "hello"
            }

            val result = GigaVoiceSettingsMapper.toApiMessage(proto)

            assertThat(result.functions).isNull()
        }

        @Test
        fun `should map API message with functions to proto`() {
            val api = ApiMessage(
                role = "assistant",
                content = "hello",
                functions = listOf(FunctionInput(name = "func1", description = "desc1"))
            )

            val result = GigaVoiceSettingsMapper.toProtoMessage(api)

            assertThat(result.functionsList).hasSize(1)
            assertThat(result.functionsList[0].name).isEqualTo("func1")
            assertThat(result.functionsList[0].description).isEqualTo("desc1")
        }

        @Test
        fun `should map API message with null functions to empty list in proto`() {
            val api = ApiMessage(role = "user", content = "hello", functions = null)

            val result = GigaVoiceSettingsMapper.toProtoMessage(api)

            assertThat(result.functionsList).isEmpty()
        }
    }
}
