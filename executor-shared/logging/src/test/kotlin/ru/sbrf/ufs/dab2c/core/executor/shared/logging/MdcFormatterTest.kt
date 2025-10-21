package ru.sbrf.ufs.dab2c.core.executor.shared.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.mdc.MdcFormatter

class MdcFormatterTest {

    @Test
    fun `test formatMdc with non null value`() {
        val expected = "[$TEST_USER_ID_KEY=$TEST_USER_ID_VALUE]"
        val result = MdcFormatter.formatMdc(TEST_USER_ID_KEY, TEST_USER_ID_VALUE)

        assertEquals(expected, result)
    }

    @Test
    fun `test formatMdc with null value`() {
        val result = MdcFormatter.formatMdc(TEST_USER_ID_KEY, null)

        assertEquals(null, result)
    }

    @Test
    fun `test toMdcParamsMap with valid map`() {
        val paramsMap = mapOf(
            TEST_USER_ID_KEY to TEST_USER_ID_VALUE,
            TEST_SESSION_ID_KEY to TEST_SESSION_ID_VALUE
        )
        val expected = mapOf(
            TEST_USER_ID_KEY to "[$TEST_USER_ID_KEY=$TEST_USER_ID_VALUE]",
            TEST_SESSION_ID_KEY to "[$TEST_SESSION_ID_KEY=$TEST_SESSION_ID_VALUE]"
        )
        val result = MdcFormatter.toMdcParamsMap(paramsMap)

        assertEquals(expected, result)
    }

    @Test
    fun `test toMdcParamsMap with empty map`() {
        val paramsMap = emptyMap<String, String>()
        val expected = emptyMap<String, String>()
        val result = MdcFormatter.toMdcParamsMap(paramsMap)

        assertEquals(expected, result)
    }

    companion object {
        const val TEST_USER_ID_KEY = "userId"
        const val TEST_USER_ID_VALUE = "12345"
        const val TEST_SESSION_ID_KEY = "sessionId"
        const val TEST_SESSION_ID_VALUE = "abcde"
    }
}
