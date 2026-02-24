package ru.sbrf.dab2c.executor.library.context

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo

class ContextAccessorsTest {

    @Test
    fun `currentHeaders should return headers from context`() = runTest {
        val headers = Headers(mapOf("x-session" to "s1", "x-token" to "t1"))

        withContext(HeadersElement(headers)) {
            val result = currentHeaders()
            assertEquals("s1", result.getHeader(RequestHeader.SESSION))
        }
    }

    @Test
    fun `currentHeaders should throw when not in context`() = runTest {
        assertThrows<IllegalStateException> {
            currentHeaders()
        }
    }

    @Test
    fun `currentSessionInfo should return session info from context`() = runTest {
        val sessionInfo = createDaSessionInfo()

        withContext(SessionInfoElement(sessionInfo)) {
            val result = currentSessionInfo()
            assertEquals("test-session", result.meta.sessionId)
        }
    }

    @Test
    fun `currentSessionInfo should throw when not in context`() = runTest {
        assertThrows<IllegalStateException> {
            currentSessionInfo()
        }
    }

    @Test
    fun `currentUfsCookie should return cookie from headers in context`() = runTest {
        val headers = Headers(mapOf("x-session" to "ses-1", "x-token" to "tok-1"))

        withContext(HeadersElement(headers)) {
            assertEquals("UFS-TOKEN=tok-1;UFS-SESSION=ses-1", currentUfsCookie())
        }
    }

    @Test
    fun `combined context should provide both headers and session info`() = runTest {
        val headers = Headers(mapOf("x-session" to "s1", "x-token" to "t1"))
        val sessionInfo = createDaSessionInfo()

        withContext(HeadersElement(headers) + SessionInfoElement(sessionInfo)) {
            assertEquals("s1", currentHeaders().getHeader(RequestHeader.SESSION))
            assertEquals("test-session", currentSessionInfo().meta.sessionId)
        }
    }

    private fun createDaSessionInfo(): DaSessionInfo = DaSessionInfo(
        meta = DaSessionMeta(
            sessionId = "test-session",
            userId = "test-user",
            ucpId = "test-ucp",
            ufsHost = "test-host"
        ),
        common = DaSessionCommon(channel = "test"),
        userInfo = DaSessionUserInfo()
    )
}
