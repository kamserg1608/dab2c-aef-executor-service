package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api

/**
 * Interface used to work with CBUL, decoupling layered cache from CBUL client interfaces.
 */
interface CbulDaoService {

    /**
     * Persists given data under given key.
     *
     * @param key key under which the data should be saved
     * @param data data to save
     *
     * @return key record under which given data has been saved
     * @throws CbulConstraintViolationException when key constraint is violated
     */
    fun saveOrUpdate(key: KeyRecord, data: String): KeyRecord

    /**
     * Persists or removes given data for a given key using Batch processing.
     * All keyRecords should relate to the same key.
     *
     * @param listBatchCbulData keys and data that need to be updated or removed in CBUL
     */
    fun batchChange(listBatchCbulData: List<BatchCbulData>)

    /**
     * Removes data persisted under given key.
     *
     * @param key key of interest
     */
    fun remove(key: KeyRecord)

    /**
     * Reads data persisted under given key.
     *
     * @param key of interest
     *
     * @return a sequence of [ValueRecord] persisted under given key
     */
    fun readData(key: KeyRecord): Sequence<ValueRecord>

    /**
     * Generates new unique row identifier.
     *
     * @return generated unique row identifier
     */
    fun generateUniqueRowId(): String

    /**
     * Retrieves data version for given [key].
     *
     * @return data version for given [key]
     */
    fun getDataVersion(key: String): Long
}
