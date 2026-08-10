package ru.sbrf.dab2c.executor.application.logging

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import ru.sbrf.dab2c.executor.logging.AdditionalMdcConfig
import ru.sbrf.dab2c.executor.logging.MaskingCollector
import ru.sbrf.dab2c.executor.logging.MdcContext
import ru.sbrf.dab2c.executor.logging.MdcKeys

class AdditionalMdcConfigTest {

    @AfterEach
    fun reset() {
        AdditionalMdcConfig.configure(emptyMap())
        MDC.clear()
    }

    @Test
    fun `MdcContext initialize should put additional keys into MDC`() {
        AdditionalMdcConfig.configure(mapOf("deploymentUnit" to "executor", "block" to "none"))

        MdcContext.initialize(serviceName = "test")

        assertEquals("executor", MDC.get("deploymentUnit"))
        assertEquals("none", MDC.get("block"))
    }

    @Test
    fun `additional keys should not appear when not configured`() {
        MdcContext.initialize(serviceName = "test")

        assertEquals(null, MDC.get("deploymentUnit"))
        assertEquals(null, MDC.get("block"))
    }

    @Test
    fun `MdcContext initialize should clear data from previous request`() {
        MDC.put(MdcKeys.TRACE_ID, "previous-trace")
        MaskingCollector.register("previous-token")

        MdcContext.initialize(serviceName = "test")

        assertNull(MDC.get(MdcKeys.TRACE_ID))
        assertNull(MDC.get(MaskingCollector.MDC_KEY))
        assertEquals("test", MDC.get(MdcKeys.SERVICE_NAME))
    }
}
