package ru.sbrf.dab2c.executor.domain.session

/**
 * Session data storage (SDS) section.
 */
data class SdsSection(
    val sectionName: String,
    val attributeName: String,
    val data: String? = null
)
