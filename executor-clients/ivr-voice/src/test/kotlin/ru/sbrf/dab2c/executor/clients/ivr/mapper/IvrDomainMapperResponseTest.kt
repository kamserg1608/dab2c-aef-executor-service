package ru.sbrf.dab2c.executor.clients.ivr.mapper

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.sbrf.dab2c.executor.clients.ivr.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import java.util.stream.Stream

/**
 * Parameterized tests for IvrDomainMapper.toProtoResponse() method.
 * Tests Domain -> Proto conversion for all response types.
 */
class IvrDomainMapperResponseTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("responseTestCases")
    fun `should convert domain response to proto`(
        @Suppress("UNUSED_PARAMETER") testName: String,
        domainResponse: VoiceResponse,
        expectedProto: GigaVoiceResponse
    ) {
        val result = IvrDomainMapper.toProtoResponse(domainResponse)

        IvrMapperTestAssertions.assertProtoEquals(result, expectedProto)
    }

    companion object {
        @JvmStatic
        fun responseTestCases(): Stream<Arguments> =
            IvrMapperTestDataLoader.loadResponseTestCases()
    }
}
