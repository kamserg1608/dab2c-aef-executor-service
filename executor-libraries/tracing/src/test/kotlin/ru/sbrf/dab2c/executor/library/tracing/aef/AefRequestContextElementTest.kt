package ru.sbrf.dab2c.executor.library.tracing.aef

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ru.sbrf.aef.observability.aiservice.AefRequestContext
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.HeadersElement

class AefRequestContextElementTest {

    @AfterEach
    fun resetSdkTl() {
        AefRequestContext.clear()
    }

    @Test
    fun `element pushes x-session-id from HeadersElement on resumption`() = runTest {
        val headers = Headers(mapOf("x-session-id" to "sess-99", "x-channel" to "IVR"))
        withContext(HeadersElement(headers) + AefRequestContextElement()) {
            assertEquals("sess-99", AefRequestContext.getSessionId())
            withContext(Dispatchers.Default) {
                assertEquals("sess-99", AefRequestContext.getSessionId())
            }
        }
        assertNull(AefRequestContext.getSessionId())
    }

    @Test
    fun `element yields null sessionId when x-session-id absent`() = runTest {
        val headers = Headers(mapOf("x-session" to "ignored", "x-channel" to "IVR"))
        withContext(HeadersElement(headers) + AefRequestContextElement()) {
            assertNull(AefRequestContext.getSessionId())
        }
    }

    @Test
    fun `element yields null when HeadersElement absent`() = runTest {
        withContext(AefRequestContextElement()) {
            assertNull(AefRequestContext.getSessionId())
        }
    }

    @Test
    fun `element restores prior ThreadLocal on suspension`() = runTest {
        AefRequestContext.setHeaders(mapOf("x-session-id" to "outer"))
        try {
            val inner = Headers(mapOf("x-session-id" to "inner"))
            withContext(HeadersElement(inner) + AefRequestContextElement()) {
                assertEquals("inner", AefRequestContext.getSessionId())
            }
            assertEquals("outer", AefRequestContext.getSessionId())
        } finally {
            AefRequestContext.clear()
        }
    }
}
