package ru.sbrf.dab2c.executor.clients.ivr.mapper

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.sbrf.dab2c.executor.clients.ivr.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import java.util.stream.Stream

/**
 * Parameterized tests for IvrDomainMapper.toDomainRequest() method.
 * Tests Proto -> Domain conversion for all request types.
 */
class IvrDomainMapperRequestTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("requestTestCases")
    fun `should convert proto request to domain`(
        @Suppress("UNUSED_PARAMETER") testName: String,
        protoRequest: GigaVoiceRequest,
        expectedDomain: VoiceRequest
    ) {
        val result = IvrDomainMapper.toDomainRequest(protoRequest)

        IvrMapperTestAssertions.assertVoiceRequestEquals(result, expectedDomain)
    }

    companion object {
        @JvmStatic
        fun requestTestCases(): Stream<Arguments> =
            IvrMapperTestDataLoader.loadRequestTestCases()
    }
}
