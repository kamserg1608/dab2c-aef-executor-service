package ru.sbrf.dab2c.executor.voice.grpc.context

import io.grpc.Metadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.voice.util.extensions.toHeaders

class GrpcMetadataContextTest {

    @Test
    fun `toHeaders should convert metadata to Headers`() {
        val metadata = Metadata()
        val key1 = Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER)
        val key2 = Metadata.Key.of("x-session-token", Metadata.ASCII_STRING_MARSHALLER)
        metadata.put(key1, "req-123")
        metadata.put(key2, "token-abc")

        val result = metadata.toHeaders()

        assertEquals("req-123", result["x-request-id"])
        assertEquals("token-abc", result["x-session-token"])
        assertNotNull(result.getHeaderOrNull(RequestHeader.X_TRACE_ID))
        assertEquals(3, result.size)
    }

    @Test
    fun `toHeaders should use last value for duplicate keys`() {
        val metadata = Metadata()
        val key = Metadata.Key.of("x-value", Metadata.ASCII_STRING_MARSHALLER)
        metadata.put(key, "first")
        metadata.put(key, "second")
        metadata.put(key, "third")

        val result = metadata.toHeaders()

        assertEquals("third", result["x-value"])
    }

    @Test
    fun `toHeaders should skip binary keys`() {
        val metadata = Metadata()
        val asciiKey = Metadata.Key.of("x-text", Metadata.ASCII_STRING_MARSHALLER)
        val binaryKey = Metadata.Key.of("x-data-bin", Metadata.BINARY_BYTE_MARSHALLER)
        metadata.put(asciiKey, "text-value")
        metadata.put(binaryKey, "binary".toByteArray())

        val result = metadata.toHeaders()

        assertTrue(result.containsKey("x-text"))
        assertTrue(!result.containsKey("x-data-bin"))
    }

    @Test
    fun `toHeaders should generate x-request-id and x-trace-id for empty metadata`() {
        val metadata = Metadata()

        val result = metadata.toHeaders()

        assertEquals(2, result.size)
        assertNotNull(result.getHeader(RequestHeader.X_REQUEST_ID))
        assertNotNull(result.getHeader(RequestHeader.X_TRACE_ID))
    }

    @Test
    fun `fromGrpcThread should return EMPTY with generated ids when no context is set`() {
        val result = GrpcMetadataContext.fromGrpcThread()

        assertEquals(2, result.size)
        assertNotNull(result.getHeader(RequestHeader.X_REQUEST_ID))
        assertNotNull(result.getHeader(RequestHeader.X_TRACE_ID))
    }
}
