package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api

import java.time.LocalDateTime

/**
 * Represents value stored in CBUL.
 */
data class ValueRecord(
    /**
     * Key record of a value stored in CBUL.
     */
    val key: KeyRecord,

    /**
     * Values stored in CBUL under an associated key record.
     */
    val data: String,

    /**
     * Creation date.
     */
    val creationDate: LocalDateTime = LocalDateTime.now(),

    /**
     * Deleted flag.
     */
    val deleted: Boolean
)
