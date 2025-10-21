package ru.sbrf.ufs.dab2c.core.executor.shared.cbul

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.CbulConstraintViolationException
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.KeyRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.CbulValidator

class CbulValidatorTest {
    private val validator = CbulValidator

    @Test
    fun `should not throw exception when all fields are valid`() {
        val key = KeyRecord("key", "alias", "uuid", 1)
        assertDoesNotThrow { validator.validateKeyConstraints(key) }
    }

    @Test
    fun `should throw exception when key is too long`() {
        val key = KeyRecord("a".repeat(129), "alias", "uuid", 1)
        assertThrows<CbulConstraintViolationException> {
            validator.validateKeyConstraints(key)
        }
    }

    @Test
    fun `should throw exception when uniqueRowId is too long`() {
        val key = KeyRecord("key", "alias", "a".repeat(129), 1)
        assertThrows<CbulConstraintViolationException> {
            validator.validateKeyConstraints(key)
        }
    }

    @Test
    fun `should allow null values for optional fields`() {
        val key = KeyRecord("key", "alias", null, 1)
        assertDoesNotThrow { validator.validateKeyConstraints(key) }
    }
}
