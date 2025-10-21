package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulData
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulOperation
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.CbulBulkReadDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.KeyRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.ManagedCbulDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.ValueRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private fun <K, V> concurrentMap(): ConcurrentHashMap<K, V> = ConcurrentHashMap()

/**
 * In-memory [CbulBulkReadDaoService] implementation that stores data in local RAM.
 * Recommended to use in tests only.
 */
@Suppress("TooManyFunctions")
open class InMemoryCbulDaoService(
    private val localTimeProvider: LocalTimeProvider,
    private val objectMapper: ObjectMapper,
) : ManagedCbulDaoService {

    private var storage: MutableMap<String, MutableMap<String, MutableMap<String, ValueRecord>>> = concurrentMap()
    private var versionStorage: MutableMap<String, AtomicLong> = concurrentMap()

    override fun saveOrUpdate(key: KeyRecord, data: String): KeyRecord {
        CbulValidator.validateKeyConstraints(key)
        val uniqueRowId = key.uniqueRowId ?: generateUniqueRowId()
        incrementVersion(key)
        val targetKey = KeyRecord(key.key, key.alias, uniqueRowId, getDataVersion(key.key))
        getOrCreateCbulKeyMap(key)[uniqueRowId] = ValueRecord(targetKey, data, localTimeProvider.now(), false)
        return targetKey
    }

    override fun batchChange(listBatchCbulData: List<BatchCbulData>) {
        listBatchCbulData.forEach {
            when (it.operation) {
                BatchCbulOperation.WRITE -> saveOrUpdate(it.key, it.data)
                BatchCbulOperation.REMOVE -> remove(it.key)
            }
        }
    }

    override fun generateUniqueRowId() = UUID.randomUUID().toString()

    override fun getDataVersion(key: String): Long =
        getMutableDataVersion(key).get()

    override fun remove(key: KeyRecord) {
        key.uniqueRowId!!
        getOrCreateCbulKeyMap(key).computeIfPresent(key.uniqueRowId) { _, value ->
            ValueRecord(value.key, value.data, value.creationDate, true)
        }
        incrementVersion(key)
    }

    private fun incrementVersion(key: KeyRecord) {
        getMutableDataVersion(key.key).incrementAndGet()
    }

    private fun getMutableDataVersion(key: String) = versionStorage.computeIfAbsent(key) { _ -> AtomicLong() }

    override fun readDataByAliases(key: String, aliases: Iterable<String>): Sequence<ValueRecord> =
        aliases.asSequence().flatMap { alias ->
            readData(KeyRecord(key, alias))
        }

    override fun readData(key: KeyRecord): Sequence<ValueRecord> {
        if (key.uniqueRowId == null) {
            return getOrCreateCbulKeyMap(key).values.asSequence()
        }
        return getOrCreateCbulKeyMap(key).values.asSequence()
            .filter { it.key.uniqueRowId == key.uniqueRowId }
    }

    private fun getOrCreateCbulKeyMap(key: KeyRecord) =
        storage.computeIfAbsent(key.key) {
            concurrentMap()
        }.computeIfAbsent(key.alias) {
            concurrentMap()
        }

    /**
     * Returns amount of shard keys being stored.
     *
     * @return amount of shard keys being stored
     */
    fun size() = storage.size

    /**
     * Returns amount of aliases being stored under a given shard key.
     *
     * @param key shard key of interest
     * @return amount of aliases being stored under a given shard key
     */
    fun sizeByAlias(key: String) = storage[key]?.size ?: 0

    /**
     * Returns amount of records stored under given shard key and alias.
     *
     * @param key shard key
     * @param alias alias inside a given shard key
     *
     * @return amount of records stored under given shard key and alias
     */
    fun sizeByKeyAndAlias(key: String, alias: String) = storage[key]?.get(alias)?.size ?: 0

    override fun resetStorage() {
        this.storage = concurrentMap()
        this.versionStorage = concurrentMap()
    }

    override fun setStorage(serializedStorage: String) {
        val typeFactory: TypeFactory = objectMapper.typeFactory
        val stringType = typeFactory.constructType(String::class.java)
        val valueRecordType = typeFactory.constructType(ValueRecord::class.java)
        val innerMapType = typeFactory.constructMapLikeType(ConcurrentHashMap::class.java, stringType, valueRecordType)
        val middleMapType = typeFactory.constructMapType(ConcurrentHashMap::class.java, stringType, innerMapType)
        val storageType = typeFactory.constructMapType(ConcurrentHashMap::class.java, stringType, middleMapType)

        storage = objectMapper.readValue(serializedStorage, storageType)
    }

    override fun setVersionStorage(serializedStorage: String) {
        val typeFactory: TypeFactory = objectMapper.typeFactory
        val stringType = typeFactory.constructType(String::class.java)
        val atomicLongType = typeFactory.constructType(AtomicLong::class.java)
        val versionStorageType = typeFactory.constructMapType(ConcurrentHashMap::class.java, stringType, atomicLongType)

        versionStorage = objectMapper.readValue(serializedStorage, versionStorageType)
    }

    override fun getStorageAsString(): String = objectMapper.writeValueAsString(storage)
}
