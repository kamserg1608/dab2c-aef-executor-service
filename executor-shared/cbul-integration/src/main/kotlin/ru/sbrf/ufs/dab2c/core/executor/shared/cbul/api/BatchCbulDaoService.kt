package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api

/**
 * Interface for batch write operations in the CBUL DAO service layer.
 *
 * This interface extends [CbulDaoService] to provide additional functionality specifically related to batch processing,
 * such as flushing cached data or persisting a collection of entities at once.
 */
internal interface BatchCbulDaoService : CbulDaoService {
    /**
     * Flushes any pending writes to the underlying storage system.
     *
     * This method ensures that all buffered changes are committed to the CBUL immediately.
     */
    fun flush()
}
