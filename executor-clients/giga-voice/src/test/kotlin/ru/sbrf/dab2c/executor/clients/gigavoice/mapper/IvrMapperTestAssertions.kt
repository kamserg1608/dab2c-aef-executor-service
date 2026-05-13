package ru.sbrf.dab2c.executor.clients.gigavoice.mapper

import org.assertj.core.api.Assertions.assertThat
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest

/**
 * Custom assertions for IvrDomainMapper tests.
 * Handles ByteArray comparison which uses reference equality by default.
 */
object IvrMapperTestAssertions {

    /**
     * Asserts that two VoiceRequest objects are equal.
     * Special handling for Audio requests which contain ByteArray.
     */
    fun assertVoiceRequestEquals(actual: VoiceRequest, expected: VoiceRequest) {
        if (actual is VoiceRequest.Audio && expected is VoiceRequest.Audio) {
            assertThat(actual.content.audioChunk).isEqualTo(expected.content.audioChunk)
            assertThat(actual.content.speechStart).isEqualTo(expected.content.speechStart)
            assertThat(actual.content.speechEnd).isEqualTo(expected.content.speechEnd)
        } else {
            assertThat(actual).isEqualTo(expected)
        }
    }

    /**
     * Asserts that two GigaVoiceResponse proto messages are equal.
     * Proto messages have proper equals() implementation.
     */
    fun assertProtoEquals(actual: GigaVoiceResponse, expected: GigaVoiceResponse) {
        assertThat(actual).isEqualTo(expected)
    }
}
