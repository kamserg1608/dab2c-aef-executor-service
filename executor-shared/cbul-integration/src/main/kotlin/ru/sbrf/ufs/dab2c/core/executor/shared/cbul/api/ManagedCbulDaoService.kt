package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api

import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.InMemoryCbulDaoService

/**
 * Interface used to manage state of stub CBUL [InMemoryCbulDaoService] implementation.
 */
interface ManagedCbulDaoService : CbulBulkReadDaoService {

    /**
     * Clears all from [CbulBulkReadDaoService] storage.
     */
    fun resetStorage()

    /**
     * Set [CbulBulkReadDaoService] storage into provided state.
     */
    fun setStorage(serializedStorage: String)

    /**
     * Set [CbulBulkReadDaoService] version storage into provided state.
     */
    fun setVersionStorage(serializedStorage: String)

    /**
     * Returns [CbulBulkReadDaoService] storage in serialized format.
     */
    fun getStorageAsString(): String
}
