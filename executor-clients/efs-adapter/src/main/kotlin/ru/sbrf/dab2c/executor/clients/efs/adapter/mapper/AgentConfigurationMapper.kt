package ru.sbrf.dab2c.executor.clients.efs.adapter.mapper

import io.mcarle.konvert.api.Konverter
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.AgentConfig
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.FewShotExample
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.NeighbourAgentMeta
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.ReturnParameter
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.ReturnParameters
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.ToolMeta
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.ToolParameterProperty
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.ToolParameters
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.configuration.FewShotExample as DomainFewShotExample
import ru.sbrf.dab2c.executor.domain.configuration.NeighbourAgentMeta as DomainNeighbourAgentMeta
import ru.sbrf.dab2c.executor.domain.configuration.ReturnParameter as DomainReturnParameter
import ru.sbrf.dab2c.executor.domain.configuration.ReturnParameters as DomainReturnParameters
import ru.sbrf.dab2c.executor.domain.configuration.ToolMeta as DomainToolMeta
import ru.sbrf.dab2c.executor.domain.configuration.ToolParameterProperty as DomainToolParameterProperty
import ru.sbrf.dab2c.executor.domain.configuration.ToolParameters as DomainToolParameters

/**
 * Mapper for converting EFS adapter contract models to domain models.
 */
@Konverter
interface AgentConfigurationMapper {

    /** Converts AgentConfig to domain AgentConfiguration. */
    fun toDomain(source: AgentConfig): AgentConfiguration

    /** Converts ToolMeta to domain ToolMeta. */
    fun toDomain(source: ToolMeta): DomainToolMeta

    /** Converts ToolParameterProperty to domain ToolParameterProperty. */
    fun toDomain(source: ToolParameterProperty): DomainToolParameterProperty

    /** Converts ToolParameters to domain ToolParameters. */
    fun toDomain(source: ToolParameters): DomainToolParameters

    /** Converts FewShotExample to domain FewShotExample. */
    fun toDomain(source: FewShotExample): DomainFewShotExample

    /** Converts ReturnParameters to domain ReturnParameters. */
    fun toDomain(source: ReturnParameters): DomainReturnParameters

    /** Converts ReturnParameter to domain ReturnParameter. */
    fun toDomain(source: ReturnParameter): DomainReturnParameter

    /** Converts NeighbourAgentMeta to domain NeighbourAgentMeta. */
    fun toDomain(source: NeighbourAgentMeta): DomainNeighbourAgentMeta

    /** Provides singleton instance of the mapper. */
    companion object {
        val INSTANCE: AgentConfigurationMapper get() = AgentConfigurationMapperImpl
    }
}
