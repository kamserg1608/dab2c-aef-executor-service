package ru.sbrf.dab2c.executor.library.tracing

import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class TracingParentElementTest {

    @Test
    fun `initial state is null`() {
        assertNull(TracingParentElement().get())
    }

    @Test
    fun `set and get return the span`() {
        val element = TracingParentElement()
        val span = Span.getInvalid()
        element.set(span)
        assertSame(span, element.get())
    }

    @Test
    fun `clear resets to null`() {
        val element = TracingParentElement()
        element.set(Span.getInvalid())
        element.clear()
        assertNull(element.get())
    }

    @Test
    fun `element is accessible from coroutine context`() = runTest {
        val element = TracingParentElement()
        withContext(element) {
            assertSame(element, coroutineContext[TracingParentElement])
        }
    }

    @Test
    fun `launched coroutine shares the same element instance`() = runTest {
        val element = TracingParentElement()
        val span = Span.getInvalid()
        withContext(element) {
            element.set(span)
            val job = launch {
                assertEquals(span, coroutineContext[TracingParentElement]?.get())
            }
            job.join()
        }
    }
}
