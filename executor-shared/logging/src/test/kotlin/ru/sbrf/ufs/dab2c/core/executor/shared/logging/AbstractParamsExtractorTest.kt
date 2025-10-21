package ru.sbrf.ufs.dab2c.core.executor.shared.logging

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.extractor.impl.AbstractLogParameterExtractor
import ru.sbrf.ufs.platform.logger.Params.USER_LOGIN

@Suppress("UNCHECKED_CAST")
class AbstractParamsExtractorTest {
    private val extractor = object : AbstractLogParameterExtractor<Any>() {
        override val supportedType: Class<Any> get() = TestArg::class.java as Class<Any>

        override fun getUcpId(target: Any): String? = (target as? TestArg)?.ucpId
    }

    @Test
    fun `test extractPlatformLoggingParams`() {
        val expectedParams = mapOf(
            USER_LOGIN to USER_LOGIN
        )

        val arg = TestArg(USER_LOGIN)
        val extractedParams = extractor.extractLoggingParams(arg)

        assertEquals(expectedParams.size, extractedParams.size)
        assertEquals(expectedParams[USER_LOGIN], extractedParams[USER_LOGIN])
    }

    @Test
    fun `test extractPlatformLoggingParams with not supported arg`() {
        val unsupportedArg = 42

        val extractedParams = extractor.extractLoggingParams(unsupportedArg)

        Assertions.assertThat(extractedParams.isEmpty()).isTrue()
    }

    data class TestArg(val ucpId: String?)
}
