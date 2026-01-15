package ru.sbrf.dab2c.executor.clients.efs.adapter.mapper

import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Mapping
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.SdsSectionData
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.SdsSectionInfo
import ru.sbrf.dab2c.executor.domain.sds.SdsSection

/**
 * Mapper for converting between SDS contract models and domain models.
 */
@Konverter
interface SdsSectionMapper {

    /** Converts SdsSectionData response to domain SdsSection. */
    fun toDomain(source: SdsSectionData): SdsSection

    /** Converts domain SdsSection to SdsSectionInfo for read requests. */
    fun toSectionInfo(source: SdsSection): SdsSectionInfo

    /** Converts domain SdsSection to SdsSectionData for write requests. */
    @Mapping(source = "data", target = "data")
    fun toSectionData(source: SdsSection): SdsSectionData

    /** Provides singleton instance of the mapper. */
    companion object {
        val INSTANCE: SdsSectionMapper get() = SdsSectionMapperImpl
    }
}
