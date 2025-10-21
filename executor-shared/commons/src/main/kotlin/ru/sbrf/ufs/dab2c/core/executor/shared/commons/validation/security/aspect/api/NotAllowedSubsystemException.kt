package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api

/**
 * Custom exception for not allowed subsystem.
 */
class NotAllowedSubsystemException(message: String) : Exception(message, null, false, false)
