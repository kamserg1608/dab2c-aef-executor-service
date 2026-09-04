package ru.sbrf.dab2c.executor.library.context

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class HeadersTest {

    @Test
    fun `factory should generate x-request-id when not present`() {
        val headers = Headers(emptyMap())

        assertNotNull(headers.getHeader(RequestHeader.X_REQUEST_ID))
    }

    @Test
    fun `factory should generate x-trace-id when not present`() {
        val headers = Headers(emptyMap())

        val traceId = headers.getHeader(RequestHeader.X_TRACE_ID)
        assertDoesNotThrow { UUID.fromString(traceId) }
    }

    @Test
    fun `factory should preserve existing x-request-id`() {
        val headers = Headers(mapOf("x-request-id" to "my-id"))

        assertEquals("my-id", headers.getHeader(RequestHeader.X_REQUEST_ID))
    }

    @Test
    fun `factory should preserve existing x-trace-id`() {
        val traceId = "550e8400-e29b-41d4-a716-446655440000"
        val headers = Headers(mapOf("x-trace-id" to traceId))

        assertEquals(traceId, headers.getHeader(RequestHeader.X_TRACE_ID))
    }

    @Test
    fun `factory should convert telemetry trace id to uuid format`() {
        val headers = Headers(mapOf("x-trace-id" to "7fba8f6f5a5bbec643deb609acc4deda"))

        assertEquals(
            "7fba8f6f-5a5b-bec6-43de-b609acc4deda",
            headers.getHeader(RequestHeader.X_TRACE_ID)
        )
    }

    @Test
    fun `factory should replace invalid x-trace-id with uuid`() {
        val headers = Headers(mapOf("x-trace-id" to "invalid-trace-id"))

        val traceId = headers.getHeader(RequestHeader.X_TRACE_ID)
        assertDoesNotThrow { UUID.fromString(traceId) }
    }

    @Test
    fun `getHeader should return value for present header`() {
        val headers = Headers(
            mapOf(
                "x-session" to "session-123",
                "x-token" to "token-456"
            )
        )

        assertEquals("session-123", headers.getHeader(RequestHeader.SESSION))
        assertEquals("token-456", headers.getHeader(RequestHeader.TOKEN))
    }

    @Test
    fun `getHeader should throw for missing header`() {
        val headers = Headers(emptyMap())

        val exception = assertThrows<IllegalStateException> {
            headers.getHeader(RequestHeader.SESSION)
        }
        assertEquals("x-session header is required", exception.message)
    }

    @Test
    fun `getHeaderOrNull should return null for missing header`() {
        val headers = Headers(emptyMap())

        assertNull(headers.getHeaderOrNull(RequestHeader.SESSION))
    }

    @Test
    fun `getHeaderOrNull should return value for present header`() {
        val headers = Headers(mapOf("x-channel" to "sbol"))

        assertEquals("sbol", headers.getHeaderOrNull(RequestHeader.CHANNEL))
    }

    @Test
    fun `ufsCookie should build cookie from token and session`() {
        val headers = Headers(
            mapOf(
                "x-token" to "tok-123",
                "x-session" to "ses-456"
            )
        )

        assertEquals("UFS-TOKEN=tok-123;UFS-SESSION=ses-456", headers.ufsCookie)
    }

    @Test
    fun `EMPTY should contain generated x-request-id and x-trace-id`() {
        val empty = Headers.EMPTY

        assertEquals(2, empty.size)
        assertNotNull(empty.getHeaderOrNull(RequestHeader.X_REQUEST_ID))
        assertNotNull(empty.getHeaderOrNull(RequestHeader.X_TRACE_ID))
    }

    @Test
    fun `should delegate Map operations`() {
        val headers = Headers(
            mapOf(
                "x-session" to "s1",
                "x-token" to "t1"
            )
        )

        assertEquals(4, headers.size)
        assertEquals("s1", headers["x-session"])
        assertNull(headers["non-existent"])
    }
}
