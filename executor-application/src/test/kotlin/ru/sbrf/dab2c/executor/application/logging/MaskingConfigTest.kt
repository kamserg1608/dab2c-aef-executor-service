package ru.sbrf.dab2c.executor.application.logging

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MaskingConfigTest {

    @AfterEach
    fun reset() {
        MaskingConfig.configure(keyEnabled = true, regexEnabled = true, patterns = emptyList())
    }

    @Test
    fun `configure should set toggle flags`() {
        MaskingConfig.configure(keyEnabled = false, regexEnabled = false, patterns = emptyList())

        assertEquals(false, MaskingConfig.keyMaskingEnabled)
        assertEquals(false, MaskingConfig.regexMaskingEnabled)
    }

    @Test
    fun `configure should compile regex patterns`() {
        MaskingConfig.configure(keyEnabled = true, regexEnabled = true, patterns = listOf("""(?<="f":")[^"]*"""))

        assertEquals(1, MaskingConfig.compiledPatterns.size)
    }

    @Test
    fun `configure should fail fast on invalid regex`() {
        assertThrows(Exception::class.java) {
            MaskingConfig.configure(keyEnabled = true, regexEnabled = true, patterns = listOf("[invalid"))
        }
    }
}
