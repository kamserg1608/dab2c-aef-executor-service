package ru.sbrf.dab2c.executor.clients.gigavoice.mapper

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import java.util.stream.Stream

/**
 * Parameterized tests for GigaVoiceDomainMapper.toProtoRequest() method.
 * Tests Domain -> Proto conversion for all request types.
 */
class GigaVoiceDomainMapperRequestTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("requestTestCases")
    fun `should convert domain request to proto`(
        @Suppress("UNUSED_PARAMETER") testName: String,
        domainRequest: VoiceRequest,
        expectedProto: GigaVoiceRequest
    ) {
        val result = GigaVoiceDomainMapper.toProtoRequest(domainRequest)

        GigaVoiceMapperTestAssertions.assertProtoEquals(result, expectedProto)
    }

    companion object {
        @JvmStatic
        fun requestTestCases(): Stream<Arguments> =
            GigaVoiceMapperTestDataLoader.loadRequestTestCases()
    }
}
