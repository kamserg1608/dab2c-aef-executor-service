package ru.sbrf.dab2c.executor.logging

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.MDC

class MaskingCollectorTest {

    @AfterEach
    fun cleanup() {
        MDC.clear()
    }

    @Test
    fun `register should store masked value in MDC`() {
        MaskingCollector.register("John")

        val map = MaskingCollector.fromMdc(MDC.getCopyOfContextMap())
        assertEquals("J**n", map["John"])
    }

    @Test
    fun `register should handle multiple values`() {
        MaskingCollector.register("John", "Ivanovich")

        val map = MaskingCollector.fromMdc(MDC.getCopyOfContextMap())
        assertEquals("J**n", map["John"])
        assertEquals("I*******h", map["Ivanovich"])
    }

    @Test
    fun `register should accumulate across calls`() {
        MaskingCollector.register("John")
        MaskingCollector.register("secret123")

        val map = MaskingCollector.fromMdc(MDC.getCopyOfContextMap())
        assertEquals(2, map.size)
        assertEquals("J**n", map["John"])
        assertEquals("s*******3", map["secret123"])
    }

    @Test
    fun `register should skip blank values`() {
        MaskingCollector.register("", "  ", "John")

        val map = MaskingCollector.fromMdc(MDC.getCopyOfContextMap())
        assertEquals(1, map.size)
        assertEquals("J**n", map["John"])
    }

    @Test
    fun `fromMdc should return empty map when no masking data`() {
        assertTrue(MaskingCollector.fromMdc(emptyMap()).isEmpty())
    }

    @Test
    fun `fromMdc should return empty map for null`() {
        assertTrue(MaskingCollector.fromMdc(null).isEmpty())
    }

    @Test
    fun `masking should survive MDC snapshot and restore`() {
        MaskingCollector.register("John")

        val snapshot = MDC.getCopyOfContextMap()
        MDC.clear()
        MDC.setContextMap(snapshot)

        val map = MaskingCollector.fromMdc(MDC.getCopyOfContextMap())
        assertEquals("J**n", map["John"])
    }
}
