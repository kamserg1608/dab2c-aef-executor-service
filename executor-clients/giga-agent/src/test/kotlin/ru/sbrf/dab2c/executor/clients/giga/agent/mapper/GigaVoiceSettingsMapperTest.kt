package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AudioSettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsOutput
import ru.sbrf.dab2c.executor.domain.voice.AudioSettings
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings

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
                enableEmotion = true
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
                enableEmotion = true
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
                enableEmotion = true
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
                enableEmotion = api.enableEmotion
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
        }
    }
}
