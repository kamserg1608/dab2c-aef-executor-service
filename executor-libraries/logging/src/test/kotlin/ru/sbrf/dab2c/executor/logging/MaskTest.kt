package ru.sbrf.dab2c.executor.logging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MaskTest {

    @Test
    fun `should mask regular string preserving first and last char`() {
        assertEquals("J**n", mask("John"))
    }

    @Test
    fun `should mask long string`() {
        assertEquals("I*******h", mask("Ivanovich"))
    }

    @Test
    fun `should mask single char as star`() {
        assertEquals("*", mask("A"))
    }

    @Test
    fun `should mask two chars as stars`() {
        assertEquals("**", mask("AB"))
    }

    @Test
    fun `should mask three char string`() {
        assertEquals("A*C", mask("ABC"))
    }

    @Test
    fun `should mask token`() {
        assertEquals("a*******z", mask("abc123xyz"))
    }
}
