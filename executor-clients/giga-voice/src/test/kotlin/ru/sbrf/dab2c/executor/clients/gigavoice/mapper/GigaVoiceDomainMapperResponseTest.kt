package ru.sbrf.dab2c.executor.clients.gigavoice.mapper

import GigaVoiceProtocol.GigaVoice.GigaVoiceResponse
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import java.util.stream.Stream

/**
 * Parameterized tests for GigaVoiceDomainMapper.toDomainResponse() method.
 * Tests Proto -> Domain conversion for all response types.
 */
class GigaVoiceDomainMapperResponseTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("responseTestCases")
    fun `should convert proto response to domain`(
        @Suppress("UNUSED_PARAMETER") testName: String,
        protoResponse: GigaVoiceResponse,
        expectedDomain: VoiceResponse
    ) {
        val result = GigaVoiceDomainMapper.toDomainResponse(protoResponse)

        GigaVoiceMapperTestAssertions.assertVoiceResponseEquals(result, expectedDomain)
    }

    @Test
    fun `should throw error when response is not set`() {
        val response = GigaVoiceResponse.getDefaultInstance()

        assertThatThrownBy { GigaVoiceDomainMapper.toDomainResponse(response) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Response not set")
    }

    companion object {
        @JvmStatic
        fun responseTestCases(): Stream<Arguments> =
            GigaVoiceMapperTestDataLoader.loadResponseTestCases()
    }
}
