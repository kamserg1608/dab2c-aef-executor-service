package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api

/**
 * Gets thrown when one or more CBUL key constraints are violated.
 */
class CbulConstraintViolationException(
    field: String,
    value: String,
    constraint: String
) : RuntimeException("Constraint [$constraint] violation for [$field] with [$value]!")
