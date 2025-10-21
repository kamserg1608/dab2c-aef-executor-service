package ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.api

/**
 * Represents service capable of extracting caller name from DTO of given type [T].
*/
interface CallerNameExtractorPart<T> {

    /**
     * Returns supported type class.
     */
    val supportedClass: Class<T>

    /**
     * Extracts caller name from given DTO.
     */
    fun extract(classInstance: T): String?
}
