package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api

/**
 * Types of CBUL batch operations.
 */
enum class BatchCbulOperation {
    /**
     * Write to CBUL batch (save or update).
     */
    WRITE,

    /**
     * Removes from CBUL batch.
     */
    REMOVE
}
