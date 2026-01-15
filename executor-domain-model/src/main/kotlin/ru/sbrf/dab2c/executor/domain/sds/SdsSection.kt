package ru.sbrf.dab2c.executor.domain.sds

/**
 * Session data storage (SDS) section.
 */
data class SdsSection(
    val sectionName: String,
    val attributeName: String,
    val data: String? = null
)
