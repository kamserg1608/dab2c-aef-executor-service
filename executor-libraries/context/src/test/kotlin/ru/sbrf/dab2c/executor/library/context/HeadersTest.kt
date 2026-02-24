package ru.sbrf.dab2c.executor.library.context

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HeadersTest {

    @Test
    fun `factory should generate x-request-id when not present`() {
        val headers = Headers(emptyMap())

        assertNotNull(headers.getHeader(RequestHeader.X_REQUEST_ID))
        assertEquals(1, headers.size)
    }

    @Test
    fun `factory should preserve existing x-request-id`() {
        val headers = Headers(mapOf("x-request-id" to "my-id"))

        assertEquals("my-id", headers.getHeader(RequestHeader.X_REQUEST_ID))
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
    fun `EMPTY should contain only generated x-request-id`() {
        val empty = Headers.EMPTY

        assertEquals(1, empty.size)
        assertNotNull(empty.getHeaderOrNull(RequestHeader.X_REQUEST_ID))
    }

    @Test
    fun `should delegate Map operations`() {
        val headers = Headers(
            mapOf(
                "x-session" to "s1",
                "x-token" to "t1"
            )
        )

        assertEquals(3, headers.size)
        assertEquals("s1", headers["x-session"])
        assertNull(headers["non-existent"])
    }
}
