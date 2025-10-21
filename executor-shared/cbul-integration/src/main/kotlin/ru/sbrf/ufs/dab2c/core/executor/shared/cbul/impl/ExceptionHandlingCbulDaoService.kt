package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulData
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.CbulBulkReadDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.KeyRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.ValueRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.exceptions.CbulException

/**
 * An extensible class for handling exceptions when working with a DAO service.
 * Implements the "Decorator" pattern to add exception handling.
 */
@Suppress("TooGenericExceptionCaught")
open class ExceptionHandlingCbulDaoService(private val delegate: CbulBulkReadDaoService) :
    CbulBulkReadDaoService by delegate {
    override fun saveOrUpdate(key: KeyRecord, data: String) = try {
        delegate.saveOrUpdate(key, data)
    } catch (e: Exception) {
        throw CbulException(e)
    }

    override fun batchChange(listBatchCbulData: List<BatchCbulData>) = try {
        delegate.batchChange(listBatchCbulData)
    } catch (e: Exception) {
        throw CbulException(e)
    }

    override fun remove(key: KeyRecord) = try {
        delegate.remove(key)
    } catch (e: Exception) {
        throw CbulException(e)
    }

    override fun readData(key: KeyRecord): Sequence<ValueRecord> = try {
        delegate.readData(key)
    } catch (e: Exception) {
        throw CbulException(e)
    }

    override fun generateUniqueRowId(): String = try {
        delegate.generateUniqueRowId()
    } catch (e: Exception) {
        throw CbulException(e)
    }

    override fun getDataVersion(key: String): Long = try {
        delegate.getDataVersion(key)
    } catch (e: Exception) {
        throw CbulException(e)
    }

    override fun readDataByAliases(key: String, aliases: Iterable<String>): Sequence<ValueRecord> = try {
        delegate.readDataByAliases(key, aliases)
    } catch (e: Exception) {
        throw CbulException(e)
    }
}
