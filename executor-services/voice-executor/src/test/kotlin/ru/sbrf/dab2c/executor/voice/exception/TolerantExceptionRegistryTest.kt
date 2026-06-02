package ru.sbrf.dab2c.executor.voice.exception

import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CancellationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TolerantExceptionRegistryTest {

    @Nested
    inner class IsTolerantTest {

        @Test
        fun `CancellationException is tolerant`() {
            assertThat(TolerantExceptionRegistry.isTolerant(CancellationException("cancelled"))).isTrue()
        }

        @Test
        fun `StatusException UNAVAILABLE is tolerant`() {
            val ex = StatusException(Status.UNAVAILABLE.withDescription("RST_STREAM closed stream"))
            assertThat(TolerantExceptionRegistry.isTolerant(ex)).isTrue()
        }

        @Test
        fun `StatusException CANCELLED is tolerant`() {
            val ex = StatusException(Status.CANCELLED.withDescription("RPC cancelled"))
            assertThat(TolerantExceptionRegistry.isTolerant(ex)).isTrue()
        }

        @Test
        fun `StatusRuntimeException UNAVAILABLE is tolerant`() {
            val ex = StatusRuntimeException(Status.UNAVAILABLE)
            assertThat(TolerantExceptionRegistry.isTolerant(ex)).isTrue()
        }

        @Test
        fun `StatusRuntimeException CANCELLED is tolerant`() {
            val ex = StatusRuntimeException(Status.CANCELLED)
            assertThat(TolerantExceptionRegistry.isTolerant(ex)).isTrue()
        }

        @Test
        fun `StatusException INTERNAL is not tolerant`() {
            val ex = StatusException(Status.INTERNAL.withDescription("server error"))
            assertThat(TolerantExceptionRegistry.isTolerant(ex)).isFalse()
        }

        @Test
        fun `StatusException with unexpected EOS description is tolerant`() {
            val ex = StatusException(
                Status.INTERNAL.withDescription("Received unexpected EOS on empty DATA frame from server")
            )
            assertThat(TolerantExceptionRegistry.isTolerant(ex)).isTrue()
        }

        @Test
        fun `StatusRuntimeException with unexpected EOS description is tolerant`() {
            val ex = StatusRuntimeException(
                Status.INTERNAL.withDescription("Received unexpected EOS on empty DATA frame from server")
            )
            assertThat(TolerantExceptionRegistry.isTolerant(ex)).isTrue()
        }

        @Test
        fun `unexpected EOS marker match is case insensitive`() {
            val ex = StatusRuntimeException(
                Status.UNKNOWN.withDescription("RECEIVED UNEXPECTED EOS ON EMPTY DATA FRAME from server")
            )
            assertThat(TolerantExceptionRegistry.isTolerant(ex)).isTrue()
        }

        @Test
        fun `StatusException UNKNOWN is not tolerant`() {
            val ex = StatusException(Status.UNKNOWN)
            assertThat(TolerantExceptionRegistry.isTolerant(ex)).isFalse()
        }

        @Test
        fun `RuntimeException is not tolerant`() {
            assertThat(TolerantExceptionRegistry.isTolerant(RuntimeException("failure"))).isFalse()
        }

        @Test
        fun `IllegalStateException is not tolerant`() {
            assertThat(TolerantExceptionRegistry.isTolerant(IllegalStateException("bad state"))).isFalse()
        }
    }

    @Nested
    inner class ExtractErrorCodeTest {

        @Test
        fun `extracts gRPC status code from StatusException`() {
            val ex = StatusException(Status.INTERNAL.withDescription("server error"))
            assertThat(TolerantExceptionRegistry.extractErrorCode(ex)).isEqualTo("INTERNAL")
        }

        @Test
        fun `extracts gRPC status code from StatusRuntimeException`() {
            val ex = StatusRuntimeException(Status.UNKNOWN)
            assertThat(TolerantExceptionRegistry.extractErrorCode(ex)).isEqualTo("UNKNOWN")
        }

        @Test
        fun `extracts class simple name for non-gRPC exceptions`() {
            assertThat(TolerantExceptionRegistry.extractErrorCode(RuntimeException("fail")))
                .isEqualTo("RuntimeException")
        }

        @Test
        fun `extracts class simple name for IllegalStateException`() {
            assertThat(TolerantExceptionRegistry.extractErrorCode(IllegalStateException("bad")))
                .isEqualTo("IllegalStateException")
        }
    }
}
