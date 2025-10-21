package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api

import org.apache.commons.lang3.StringUtils

/**
 * Represents data for batch cbul operation.
 */
data class BatchCbulData(
    /**
     * Key record.
     */
    val key: KeyRecord,

    /**
     * Data record.
     */
    val data: String = StringUtils.EMPTY,

    /**
     * Operation: write or remove.
     */
    val operation: BatchCbulOperation
)
