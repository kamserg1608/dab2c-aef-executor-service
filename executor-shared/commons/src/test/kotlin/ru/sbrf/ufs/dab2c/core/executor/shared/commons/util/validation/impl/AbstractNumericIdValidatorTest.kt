package ru.sbrf.ufs.dab2c.core.executor.shared.commons.util.validation.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.impl.AbstractNumericIdValidator

class AbstractNumericIdValidatorTest {

    private val validator = object : AbstractNumericIdValidator<TestEntity>() {
        override fun TestEntity.extractNumericId(): String? = this.numericId

        override fun violationMessage(numericId: String?): String =
            "Некорректный числовой идентификатор: $numericId"
    }

    data class TestEntity(val numericId: String?)

    @Test
    fun `should return invalid when numericId is null`() {
        val entity = TestEntity(null)
        val result = validator.validate(entity)

        assertFalse(result.valid)
        assertEquals(1, result.violations.size)
        assertEquals("Некорректный числовой идентификатор: null", result.violations[0].message)
    }

    @Test
    fun `should return invalid when numericId is empty string`() {
        val entity = TestEntity("")
        val result = validator.validate(entity)

        assertFalse(result.valid)
        assertEquals(1, result.violations.size)
        assertEquals("Некорректный числовой идентификатор: ", result.violations[0].message)
    }

    @Test
    fun `should return invalid when numericId contains non-digit characters`() {
        val entity = TestEntity("123abc")
        val result = validator.validate(entity)

        assertFalse(result.valid)
        assertEquals(1, result.violations.size)
        assertEquals("Некорректный числовой идентификатор: 123abc", result.violations[0].message)
    }

    @Test
    fun `should return valid when numericId is correct`() {
        val entity = TestEntity("789012")
        val result = validator.validate(entity)

        assertTrue(result.valid)
        assertTrue(result.violations.isEmpty())
    }

    @Test
    fun `should return invalid when numericId is blank`() {
        val entity = TestEntity("  ")
        val result = validator.validate(entity)

        assertFalse(result.valid)
        assertEquals(1, result.violations.size)
        assertEquals("Некорректный числовой идентификатор:   ", result.violations[0].message)
    }
}
