package ru.sbrf.dab2c.executor.application.logging

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import ru.sbrf.dab2c.executor.logging.MaskingCollector

class MaskingTest {

    @AfterEach
    fun reset() {
        MaskingConfig.configure(keyEnabled = true, regexEnabled = true, patterns = emptyList())
        MDC.clear()
    }

    @Test
    fun `regex masking should replace matched values`() {
        MaskingConfig.configure(
            keyEnabled = true, regexEnabled = true,
            patterns = listOf("""(?<="first_name":")[^"]*""")
        )

        val result = applyMasking("""{"first_name":"John","age":30}""", null)

        assertEquals("""{"first_name":"***","age":30}""", result)
    }

    @Test
    fun `regex masking should handle camelCase field names`() {
        MaskingConfig.configure(
            keyEnabled = true, regexEnabled = true,
            patterns = listOf("""(?<="firstName":")[^"]*""")
        )

        val result = applyMasking("""{"firstName":"Ivan","lastName":"Doe"}""", null)

        assertEquals("""{"firstName":"***","lastName":"Doe"}""", result)
    }

    @Test
    fun `regex masking should apply multiple patterns`() {
        MaskingConfig.configure(
            keyEnabled = true, regexEnabled = true,
            patterns = listOf("""(?<="first_name":")[^"]*""", """(?<="patr_name":")[^"]*""")
        )

        val result = applyMasking("""{"first_name":"John","patr_name":"Ivanovich"}""", null)

        assertEquals("""{"first_name":"***","patr_name":"***"}""", result)
    }

    @Test
    fun `regex masking should handle multiple occurrences`() {
        MaskingConfig.configure(
            keyEnabled = true, regexEnabled = true,
            patterns = listOf("""(?<="firstName":")[^"]*""")
        )

        val result = applyMasking("""[{"firstName":"John"},{"firstName":"Jane"}]""", null)

        assertEquals("""[{"firstName":"***"},{"firstName":"***"}]""", result)
    }

    @Test
    fun `regex masking should return original when no match`() {
        MaskingConfig.configure(
            keyEnabled = true, regexEnabled = true,
            patterns = listOf("""(?<="first_name":")[^"]*""")
        )

        val input = """{"age":30,"city":"Moscow"}"""
        assertEquals(input, applyMasking(input, null))
    }

    @Test
    fun `should skip regex masking when disabled`() {
        MaskingConfig.configure(
            keyEnabled = true, regexEnabled = false,
            patterns = listOf("""(?<="first_name":")[^"]*""")
        )

        val input = """{"first_name":"John"}"""
        assertEquals(input, applyMasking(input, null))
    }

    @Test
    fun `should skip key masking when disabled`() {
        MaskingConfig.configure(keyEnabled = false, regexEnabled = true, patterns = emptyList())
        MaskingCollector.register("secret")

        val result = applyMasking("value is secret", MDC.getCopyOfContextMap())

        assertEquals("value is secret", result)
    }

    @Test
    fun `should apply key masking from MDC`() {
        MaskingConfig.configure(keyEnabled = true, regexEnabled = true, patterns = emptyList())
        MaskingCollector.register("John")

        val result = applyMasking("Hello John!", MDC.getCopyOfContextMap())

        assertEquals("Hello J**n!", result)
    }

    @Test
    fun `regex masking should handle escaped JSON quotes`() {
        MaskingConfig.configure(
            keyEnabled = true, regexEnabled = true,
            patterns = listOf("""(?<=\\"firstName\\":\\")[^\\"]*""", """(?<=\\"patrName\\":\\")[^\\"]*""")
        )

        val result = applyMasking(
            """{"message":"{\"firstName\":\"Всеслав\",\"patrName\":\"Владиславович\"}"}""",
            null
        )

        assertEquals("""{"message":"{\"firstName\":\"***\",\"patrName\":\"***\"}"}""", result)
    }

    @Test
    fun `should apply both key and regex masking together`() {
        MaskingConfig.configure(
            keyEnabled = true, regexEnabled = true,
            patterns = listOf("""(?<="patr_name":")[^"]*""")
        )
        MaskingCollector.register("John")

        val result = applyMasking(
            """user John sent {"patr_name":"Ivanovich"}""",
            MDC.getCopyOfContextMap()
        )

        assertEquals("""user J**n sent {"patr_name":"***"}""", result)
    }
}
