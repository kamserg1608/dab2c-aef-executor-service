package ru.sbrf.dab2c.executor.clients.efs.adapter.mapper

import io.mcarle.konvert.api.Konverter
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.FunctionConfig
import ru.sbrf.dab2c.executor.domain.configuration.FunctionConfig as DomainFunctionConfig

/**
 * Mapper for converting EFS adapter function config to domain model.
 */
@Konverter
interface FunctionConfigMapper {

    /** Converts FunctionConfig to domain FunctionConfig. */
    fun toDomain(source: FunctionConfig): DomainFunctionConfig

    /** Provides singleton instance of the mapper. */
    companion object {
        val INSTANCE: FunctionConfigMapper get() = FunctionConfigMapperImpl
    }
}
