package ru.sbrf.dab2c.executor.domain.configuration

/**
 * Session configuration retrieved from EFS Adapter.
 * Contains channel and platform information for downstream API calls.
 */
data class SessionConfiguration(
    val channel: String,
    val platform: String
)
