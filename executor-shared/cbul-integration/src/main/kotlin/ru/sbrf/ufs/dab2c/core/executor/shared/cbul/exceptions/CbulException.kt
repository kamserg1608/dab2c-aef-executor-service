package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.exceptions

/**
 * A common error meaning the Cbul response is an error.
 */
class CbulException(cause: Exception) : Exception(cause.message, cause, false, false)
