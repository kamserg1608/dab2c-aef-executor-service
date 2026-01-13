package ru.sbrf.dab2c.executor.clients.gigavoice.mapper

import GigaVoiceProtocol.GigaVoice.GigaVoiceRequest
import org.assertj.core.api.Assertions.assertThat
import ru.sbrf.dab2c.executor.domain.voice.AudioContent
import ru.sbrf.dab2c.executor.domain.voice.ContentFromModel
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse

/**
 * Custom assertions for GigaVoiceDomainMapper tests.
 * Handles ByteArray comparison (since ByteArray.equals() uses reference equality).
 */
object GigaVoiceMapperTestAssertions {

    /**
     * Asserts that two VoiceResponse objects are equal.
     * Special handling for ByteArray fields in AudioOutput.
     */
    fun assertVoiceResponseEquals(actual: VoiceResponse, expected: VoiceResponse) {
        if (actual is VoiceResponse.Output && expected is VoiceResponse.Output) {
            assertContentFromModelEquals(actual.content, expected.content)
        } else {
            assertThat(actual).isEqualTo(expected)
        }
    }

    private fun assertContentFromModelEquals(actual: ContentFromModel, expected: ContentFromModel) {
        when {
            actual is ContentFromModel.Audio && expected is ContentFromModel.Audio -> {
                assertThat(actual.audio.audioChunk)
                    .withFailMessage(
                        "AudioOutput.audioChunk mismatch:\n" +
                            "  expected: ${expected.audio.audioChunk.contentToString()}\n" +
                            "  actual: ${actual.audio.audioChunk.contentToString()}"
                    )
                    .isEqualTo(expected.audio.audioChunk)
                assertThat(actual.audio.audioDuration)
                    .withFailMessage(
                        "AudioOutput.audioDuration mismatch:\n" +
                            "  expected: ${expected.audio.audioDuration}\n" +
                            "  actual: ${actual.audio.audioDuration}"
                    )
                    .isEqualTo(expected.audio.audioDuration)
                assertThat(actual.audio.isFinal)
                    .withFailMessage(
                        "AudioOutput.isFinal mismatch:\n" +
                            "  expected: ${expected.audio.isFinal}\n" +
                            "  actual: ${actual.audio.isFinal}"
                    )
                    .isEqualTo(expected.audio.isFinal)
            }
            actual is ContentFromModel.AdditionalData && expected is ContentFromModel.AdditionalData -> {
                assertThat(actual.data).isEqualTo(expected.data)
            }
            actual is ContentFromModel.Interrupted && expected is ContentFromModel.Interrupted -> {
                // Both are Interrupted - they're equal
            }
            else -> {
                assertThat(actual::class).isEqualTo(expected::class)
                assertThat(actual).isEqualTo(expected)
            }
        }
    }

    /**
     * Asserts that two GigaVoiceRequest proto messages are equal.
     * Proto messages have proper equals() implementation.
     */
    fun assertProtoEquals(actual: GigaVoiceRequest, expected: GigaVoiceRequest) {
        assertThat(actual).isEqualTo(expected)
    }

    /**
     * Asserts that two VoiceRequest objects are equal.
     * Special handling for ByteArray fields in AudioContent.
     */
    fun assertVoiceRequestEquals(actual: VoiceRequest, expected: VoiceRequest) {
        if (actual is VoiceRequest.Audio && expected is VoiceRequest.Audio) {
            assertAudioContentEquals(actual.content, expected.content)
        } else {
            assertThat(actual).isEqualTo(expected)
        }
    }

    private fun assertAudioContentEquals(actual: AudioContent, expected: AudioContent) {
        assertThat(actual.audioChunk)
            .withFailMessage(
                "AudioContent.audioChunk mismatch:\n" +
                    "  expected: ${expected.audioChunk?.contentToString()}\n" +
                    "  actual: ${actual.audioChunk?.contentToString()}"
            )
            .isEqualTo(expected.audioChunk)
        assertThat(actual.speechStart).isEqualTo(expected.speechStart)
        assertThat(actual.speechEnd).isEqualTo(expected.speechEnd)
    }
}
