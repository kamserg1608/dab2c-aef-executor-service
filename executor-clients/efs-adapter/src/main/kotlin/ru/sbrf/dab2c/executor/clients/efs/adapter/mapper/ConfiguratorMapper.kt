package ru.sbrf.dab2c.executor.clients.efs.adapter.mapper

import io.mcarle.konvert.api.Konverter
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.SessionConfig
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon

/**
 * Mapper for converting EFS adapter contract models to domain models.
 */
@Konverter
interface ConfiguratorMapper {

    /** Converts SessionConfig to domain DaSessionCommon. */
    fun toDomain(source: SessionConfig): DaSessionCommon

    /** Provides singleton instance of the mapper. */
    companion object {
        val INSTANCE: ConfiguratorMapper get() = ConfiguratorMapperImpl
    }
}
