package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api

/**
 * Represents key record in CBUL (aka full-key).
 */
data class KeyRecord(
    /**
     * Key defining CBUL shard.
     */
    val key: String,

    /**
     * Record alias within given key.
     */
    val alias: String,

    /**
     * Unique row identifier within given key and alias.
     */
    val uniqueRowId: String? = null,

    /**
     * Data version from CBUL.
     */
    val cbulVersion: Long? = null

)
