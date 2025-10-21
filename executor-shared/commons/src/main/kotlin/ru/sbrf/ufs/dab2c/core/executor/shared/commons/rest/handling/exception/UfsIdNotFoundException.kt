package ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.exception

/**
 * Exception thrown when ufsId is not found.
 */
class UfsIdNotFoundException(message: String) : Exception(message, null, false, false)
