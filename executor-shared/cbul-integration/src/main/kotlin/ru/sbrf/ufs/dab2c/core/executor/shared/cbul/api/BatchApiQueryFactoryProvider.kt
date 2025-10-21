package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api

import ru.sbrf.cbul.client.service.BatchApiQueryFactory

/**
 * Abstraction over different methods of getting BatchApiQueryFactory.
 */
fun interface BatchApiQueryFactoryProvider {

    /**
     * Returns BatchApiQueryFactory.
     */
    fun getBatchApiQueryFactory(): BatchApiQueryFactory
}
