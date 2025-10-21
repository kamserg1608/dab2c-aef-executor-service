package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api

/**
 * Additional CBUL parameters.
 */
interface AdditionalCbulParameters {

    /**
     * Gets the CBUL batch size.
     *
     * @return the CBUL batch size
     */
    fun getCbulBatchSize(): Int
}
