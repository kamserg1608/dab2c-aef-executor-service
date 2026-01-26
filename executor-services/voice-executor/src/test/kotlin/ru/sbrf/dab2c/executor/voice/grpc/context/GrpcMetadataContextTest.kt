package ru.sbrf.dab2c.executor.voice.grpc.context

import io.grpc.Metadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext.toRequestMetadata
import ru.sbrf.dab2c.executor.voice.model.RequestHeader

class GrpcMetadataContextTest {

    @Test
    fun `toRequestMetadata should convert metadata to RequestMetadata`() {
        val metadata = Metadata()
        val key1 = Metadata.Key.of("x-request-id", Metadata.ASCII_STRING_MARSHALLER)
        val key2 = Metadata.Key.of("x-session-token", Metadata.ASCII_STRING_MARSHALLER)
        metadata.put(key1, "req-123")
        metadata.put(key2, "token-abc")

        val result = metadata.toRequestMetadata()

        assertEquals("req-123", result["x-request-id"])
        assertEquals("token-abc", result["x-session-token"])
        assertEquals(2, result.size)
    }

    @Test
    fun `toRequestMetadata should use last value for duplicate keys`() {
        val metadata = Metadata()
        val key = Metadata.Key.of("x-value", Metadata.ASCII_STRING_MARSHALLER)
        metadata.put(key, "first")
        metadata.put(key, "second")
        metadata.put(key, "third")

        val result = metadata.toRequestMetadata()

        assertEquals("third", result["x-value"])
    }

    @Test
    fun `toRequestMetadata should skip binary keys`() {
        val metadata = Metadata()
        val asciiKey = Metadata.Key.of("x-text", Metadata.ASCII_STRING_MARSHALLER)
        val binaryKey = Metadata.Key.of("x-data-bin", Metadata.BINARY_BYTE_MARSHALLER)
        metadata.put(asciiKey, "text-value")
        metadata.put(binaryKey, "binary".toByteArray())

        val result = metadata.toRequestMetadata()

        assertTrue(result.containsKey("x-text"))
        assertTrue(!result.containsKey("x-data-bin"))
    }

    @Test
    fun `toRequestMetadata should generate x-request-id for empty metadata`() {
        val metadata = Metadata()

        val result = metadata.toRequestMetadata()

        assertEquals(1, result.size)
        assertNotNull(result.getHeader(RequestHeader.X_REQUEST_ID))
    }

    @Test
    fun `current should return EMPTY with generated x-request-id when no context is set`() {
        val result = GrpcMetadataContext.current()

        assertEquals(1, result.size)
        assertNotNull(result.getHeader(RequestHeader.X_REQUEST_ID))
    }
}
