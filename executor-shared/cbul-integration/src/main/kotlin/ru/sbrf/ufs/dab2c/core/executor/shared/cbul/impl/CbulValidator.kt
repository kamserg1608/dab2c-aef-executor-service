package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.CbulConstraintViolationException
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.KeyRecord

/**
 * Validator for CBUL key record.
 */
object CbulValidator {

    private const val KEY_MAX_LENGTH = 128
    private const val ALIAS_MAX_LENGTH = 30
    private const val UUID_MAX_LENGTH = 128

    /**
     * Validates CBUL key record.
     */
    fun validateKeyConstraints(keyRecord: KeyRecord) {
        validateLength("key", keyRecord.key, KEY_MAX_LENGTH)
        validateLength("alias", keyRecord.alias, ALIAS_MAX_LENGTH)
        validateLength("uniqueRowId", keyRecord.uniqueRowId, UUID_MAX_LENGTH)
    }

    private fun validateLength(field: String, value: String?, maxLength: Int) {
        if (value != null && value.length > maxLength) {
            throw CbulConstraintViolationException(
                field = field,
                value = value,
                constraint = "max length <= $maxLength"
            )
        }
    }
}
