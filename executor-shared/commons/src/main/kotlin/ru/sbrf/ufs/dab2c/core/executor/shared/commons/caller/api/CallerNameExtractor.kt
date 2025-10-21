package ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.api

/**
 * Represents caller name extraction strategy.
 */
interface CallerNameExtractor {

    /**
     * Extracts caller name from the given target.
     *
     * @param target target to extract caller name from.
     * @return 'spName' extracted from the given target.
     */
    fun extract(target: Any?): String?
}
