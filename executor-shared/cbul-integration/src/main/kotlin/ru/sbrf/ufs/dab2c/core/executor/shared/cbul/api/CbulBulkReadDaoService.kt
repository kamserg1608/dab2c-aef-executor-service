package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api

/**
 * [CbulDaoService] extended with bulk-read capabilities.
 */
interface CbulBulkReadDaoService : CbulDaoService {

    /**
     * Read data associated with given CBUL key and supplied aliases.
     *
     * @param key CBUL key
     * @param aliases aliases
     *
     * @return a sequence of [ValueRecord] associated with given CBUL key and supplied aliases
     */
    fun readDataByAliases(key: String, aliases: Iterable<String>): Sequence<ValueRecord>
}
