package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl

import ru.sbrf.cbul.client.dto.CbulEntityStatus
import ru.sbrf.cbul.client.dto.CbulRecord
import ru.sbrf.cbul.client.exceptions.CbulNotFoundDataException
import ru.sbrf.cbul.client.service.CbulClientService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchApiQueryFactoryProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulData
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.BatchCbulOperation
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.CbulBulkReadDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.KeyRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.ValueRecord
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.impl.CbulValidator.validateKeyConstraints
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider
import java.nio.charset.Charset

/**
 * [CbulBulkReadDaoService] using [CbulClientService] as a source.
 */
@Suppress("TooManyFunctions")
class CbulDaoServiceImpl(
    private val cbulClientService: CbulClientService,
    private val localTimeProvider: LocalTimeProvider,
    private val batchApiQueryFactoryProvider: BatchApiQueryFactoryProvider
) : CbulBulkReadDaoService {

    override fun saveOrUpdate(key: KeyRecord, data: String): KeyRecord {
        validateKeyConstraints(key)
        val uniqueRowId = key.uniqueRowId ?: generateUniqueRowId()
        val cbulVersion = cbulClientService.addOrUpdateRow(
            key.key,
            key.alias,
            localTimeProvider.now(),
            uniqueRowId,
            data
        )
        return KeyRecord(key.key, key.alias, uniqueRowId, cbulVersion)
    }

    override fun batchChange(listBatchCbulData: List<BatchCbulData>) {
        check(listBatchCbulData.map { it.key.key }.distinct().size == 1) {
            UNIQUE_KEY_CONSTRAINT_VIOLATION_ERROR_MESSAGE
        }
        batchApiQueryFactoryProvider.getBatchApiQueryFactory()
            .newQuery(listBatchCbulData.first().key.key, listBatchCbulData.size)
            .apply {
                listBatchCbulData.forEach {
                    when (it.operation) {
                        BatchCbulOperation.WRITE -> {
                            validateKeyConstraints(it.key)
                            val uniqueRowId = it.key.uniqueRowId ?: generateUniqueRowId()
                            addOrUpdateRow(it.key.alias, localTimeProvider.now(), uniqueRowId, it.data)
                        }
                        BatchCbulOperation.REMOVE -> {
                            validateKeyConstraints(it.key)
                            deleteRow(it.key.alias, it.key.uniqueRowId)
                        }
                    }
                }
            }
            .flushCommands()
    }

    override fun generateUniqueRowId(): String = cbulClientService.generateUniqueRowId()

    override fun getDataVersion(key: String): Long = cbulClientService.getDataVersion(key)

    override fun remove(key: KeyRecord) {
        cbulClientService.deleteRow(key.key, key.alias, key.uniqueRowId!!)
    }

    override fun readDataByAliases(key: String, aliases: Iterable<String>): Sequence<ValueRecord> =
        cbulClientService.readDataByAliases(key, aliases.toList().toTypedArray())
            .asSequence()
            .toValueRecordSequence()

    override fun readData(key: KeyRecord): Sequence<ValueRecord> =
        getCbulRecordSequence(key).toValueRecordSequence()

    private fun getCbulRecordSequence(key: KeyRecord): Sequence<CbulRecord> =
        if (key.uniqueRowId != null) {
            readSingleRow(key)
        } else {
            cbulClientService.readDataByAliases(key.key, arrayOf(key.alias)).asSequence()
        }

    @Suppress("SwallowedException")
    private fun readSingleRow(key: KeyRecord) = try {
        sequenceOf(
            cbulClientService.readRecord(
                key.key, key.alias, key.uniqueRowId
            )
        )
    } catch (e: CbulNotFoundDataException) {
        emptySequence()
    }

    private fun Sequence<CbulRecord>.toValueRecordSequence() = this.map { toValueRecord(it) }

    private fun toValueRecord(
        cbulRecord: CbulRecord
    ) = ValueRecord(
        KeyRecord(
            cbulRecord.cbulId, cbulRecord.alias, cbulRecord.uuid, cbulRecord.version
        ),
        String(cbulRecord.data, Charset.forName(cbulRecord.charset)),
        cbulRecord.creationDate,
        isDeleted(cbulRecord)
    )

    private fun isDeleted(cbulRecord: CbulRecord) = cbulRecord.status == CbulEntityStatus.DELETED

    private companion object {
        private const val UNIQUE_KEY_CONSTRAINT_VIOLATION_ERROR_MESSAGE =
            "Batch should contain only records with the same key"
    }
}
