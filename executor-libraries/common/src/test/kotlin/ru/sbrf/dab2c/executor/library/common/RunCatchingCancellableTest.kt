package ru.sbrf.dab2c.executor.library.common

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RunCatchingCancellableTest {

    @Test
    fun `returns success on normal completion`() {
        val result = runCatchingCancellable { 42 }
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `wraps exception into failure`() {
        val result = runCatchingCancellable<Int> { error("boom") }
        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }

    @Test
    fun `rethrows CancellationException`() = runTest {
        assertThrows<CancellationException> {
            runCatchingCancellable { throw CancellationException("cancel") }
        }
    }
}
