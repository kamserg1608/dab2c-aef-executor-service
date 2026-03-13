package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AudioSettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.StubSoundsOutput
import ru.sbrf.dab2c.executor.domain.voice.AudioSettings
import ru.sbrf.dab2c.executor.domain.voice.FunctionDefinition
import ru.sbrf.dab2c.executor.domain.voice.Message
import ru.sbrf.dab2c.executor.domain.voice.StubSounds
import ru.sbrf.dab2c.executor.domain.voice.TriggerFunctionMode
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import kotlin.time.Duration.Companion.seconds
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Message as ApiMessage
import ru.sbrf.dab2c.executor.clients.giga.agent.model.TriggerFunction as ApiTriggerFunction
import ru.sbrf.dab2c.executor.clients.giga.agent.model.TriggerFunctionMode as ApiTriggerFunctionMode
import ru.sbrf.dab2c.executor.clients.giga.agent.model.TriggerGeneration as ApiTriggerGeneration
import ru.sbrf.dab2c.executor.domain.voice.TriggerFunction as DomainTriggerFunction
import ru.sbrf.dab2c.executor.domain.voice.TriggerGeneration as DomainTriggerGeneration

/**
 * Tests for [GigaVoiceSettingsMapper] focusing on boolean flag and flags list mappings.
 */
class GigaVoiceSettingsMapperTest {

    @Nested
    inner class ToApiSettingsInput {

        @Test
        fun `should map all boolean flags and flags list when populated`() {
            val domain = VoiceSettings(
                voiceCallId = "test-call-id",
                audio = AudioSettings(),
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

            val result = GigaVoiceSettingsMapper.toApiSettingsInput(domain)

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
        fun `should map default domain values correctly`() {
            val domain = VoiceSettings(
                voiceCallId = "test-call-id",
                audio = AudioSettings()
            )

            val result = GigaVoiceSettingsMapper.toApiSettingsInput(domain)

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
    inner class ToDomainSettings {

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

            val result = GigaVoiceSettingsMapper.toDomainSettings(api)

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
        fun `should use defaults when API fields are null`() {
            val api = SettingsOutput(
                voiceCallId = "test-call-id",
                audio = AudioSettingsOutput()
            )

            val result = GigaVoiceSettingsMapper.toDomainSettings(api)

            assertThat(result.disableVad).isFalse()
            assertThat(result.enableTranscribeInput).isFalse()
            assertThat(result.flags).isEmpty()
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
        fun `should preserve all fields through domain to API to domain conversion`() {
            val original = VoiceSettings(
                voiceCallId = "round-trip-id",
                audio = AudioSettings(),
                disableVad = true,
                enableTranscribeInput = true,
                flags = listOf("alpha", "beta"),
                enableDenoiser = true,
                enablePrefetch = false,
                enablePersonIdentity = true,
                enableWhisper = false,
                enableEmotion = true,
                enableTranscribeSilencePhrases = true
            )

            val api = GigaVoiceSettingsMapper.toApiSettingsInput(original)
            val apiOutput = SettingsOutput(
                voiceCallId = api.voiceCallId,
                audio = AudioSettingsOutput(),
                disableVad = api.disableVad,
                enableTranscribeInput = api.enableTranscribeInput,
                flags = api.flags,
                enableDenoiser = api.enableDenoiser,
                enablePrefetch = api.enablePrefetch,
                enablePersonIdentity = api.enablePersonIdentity,
                enableWhisper = api.enableWhisper,
                enableEmotion = api.enableEmotion,
                enableTranscribeSilencePhrases = api.enableTranscribeSilencePhrases
            )
            val result = GigaVoiceSettingsMapper.toDomainSettings(apiOutput)

            assertThat(result.disableVad).isEqualTo(original.disableVad)
            assertThat(result.enableTranscribeInput).isEqualTo(original.enableTranscribeInput)
            assertThat(result.flags).isEqualTo(original.flags)
            assertThat(result.enableDenoiser).isEqualTo(original.enableDenoiser)
            assertThat(result.enablePrefetch).isEqualTo(original.enablePrefetch)
            assertThat(result.enablePersonIdentity).isEqualTo(original.enablePersonIdentity)
            assertThat(result.enableWhisper).isEqualTo(original.enableWhisper)
            assertThat(result.enableEmotion).isEqualTo(original.enableEmotion)
            assertThat(result.enableTranscribeSilencePhrases).isEqualTo(original.enableTranscribeSilencePhrases)
        }
    }

    @Nested
    inner class StubSoundsMapping {

        @Test
        fun `should map domain StubSounds with triggerGeneration to API`() {
            val domain = StubSounds(
                triggerGeneration = DomainTriggerGeneration(timeout = 30.seconds, enable = true),
                triggerFunction = DomainTriggerFunction(
                    enable = true,
                    mode = TriggerFunctionMode.WHITELIST,
                    functionNames = listOf("func1")
                ),
                sounds = listOf("sound1", "sound2")
            )

            val result = GigaVoiceSettingsMapper.toApiStubSoundsInput(domain)

            assertThat(result.triggerGeneration!!.timeout).isEqualTo("30s")
            assertThat(result.triggerGeneration!!.enable).isTrue()
            assertThat(result.triggerFunction!!.enable).isTrue()
            assertThat(result.sounds).containsExactly("sound1", "sound2")
        }

        @Test
        fun `should use defaults when domain triggerGeneration is null`() {
            val domain = StubSounds(sounds = listOf("sound1"))

            val result = GigaVoiceSettingsMapper.toApiStubSoundsInput(domain)

            assertThat(result.triggerGeneration).isNull()
        }

        @Test
        fun `should use defaults when domain triggerFunction is null`() {
            val domain = StubSounds(sounds = listOf("sound1"))

            val result = GigaVoiceSettingsMapper.toApiStubSoundsInput(domain)

            assertThat(result.triggerFunction).isNull()
        }

        @Test
        fun `should map API StubSoundsOutput with triggerGeneration to domain`() {
            val api = StubSoundsOutput(
                triggerGeneration = ApiTriggerGeneration(timeout = "15s", enable = true),
                triggerFunction = ApiTriggerFunction(
                    enable = true,
                    mode = ApiTriggerFunctionMode._1,
                    functionNames = listOf("func1")
                ),
                sounds = listOf("sound1")
            )

            val result = GigaVoiceSettingsMapper.toDomainStubSounds(api)

            assertThat(result.triggerGeneration).isNotNull
            assertThat(result.triggerGeneration!!.timeout).isEqualTo(15.seconds)
            assertThat(result.triggerGeneration!!.enable).isTrue()
            assertThat(result.triggerFunction).isNotNull
            assertThat(result.triggerFunction!!.enable).isTrue()
            assertThat(result.sounds).containsExactly("sound1")
        }

        @Test
        fun `should map null triggerGeneration in API to null in domain`() {
            val api = StubSoundsOutput(
                triggerFunction = ApiTriggerFunction(
                    enable = false,
                    mode = ApiTriggerFunctionMode._0,
                    functionNames = emptyList()
                ),
                sounds = emptyList()
            )

            val result = GigaVoiceSettingsMapper.toDomainStubSounds(api)

            assertThat(result.triggerGeneration).isNull()
        }
    }

    @Nested
    inner class MessageMapping {

        @Test
        fun `should map domain message with functions to API`() {
            val domain = Message(
                role = "assistant",
                content = "hello",
                functions = listOf(
                    FunctionDefinition(name = "func1", description = "desc1")
                )
            )

            val result = GigaVoiceSettingsMapper.toApiMessage(domain)

            assertThat(result.functions).hasSize(1)
            assertThat(result.functions!![0].name).isEqualTo("func1")
            assertThat(result.functions!![0].description).isEqualTo("desc1")
        }

        @Test
        fun `should map domain message with empty functions to null in API`() {
            val domain = Message(
                role = "user",
                content = "hello",
                functions = emptyList()
            )

            val result = GigaVoiceSettingsMapper.toApiMessage(domain)

            assertThat(result.functions).isNull()
        }

        @Test
        fun `should map API message with functions to domain`() {
            val api = ApiMessage(
                role = "assistant",
                content = "hello",
                functions = listOf(
                    FunctionInput(name = "func1", description = "desc1")
                )
            )

            val result = GigaVoiceSettingsMapper.toDomainMessage(api)

            assertThat(result.functions).hasSize(1)
            assertThat(result.functions[0].name).isEqualTo("func1")
            assertThat(result.functions[0].description).isEqualTo("desc1")
        }

        @Test
        fun `should map API message with null functions to empty list in domain`() {
            val api = ApiMessage(
                role = "user",
                content = "hello",
                functions = null
            )

            val result = GigaVoiceSettingsMapper.toDomainMessage(api)

            assertThat(result.functions).isEmpty()
        }
    }
}
