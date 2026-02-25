package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import ru.sbrf.dab2c.executor.clients.giga.agent.model.AgentConfig
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.configuration.NeighbourAgentMeta
import ru.sbrf.dab2c.executor.domain.configuration.ToolMeta
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Manual mapper for converting domain AgentConfiguration to API AgentConfig.
 * Uses Jackson ObjectMapper for converting typed domain models to untyped Map.
 */
object GigaVoiceAgentConfigMapper {

    /**
     * Converts domain AgentConfiguration to API AgentConfig.
     */
    @Suppress("UNCHECKED_CAST")
    fun toApiAgentConfig(source: AgentConfiguration): AgentConfig = AgentConfig(
        name = source.name,
        type = source.type,
        functionalSubsystemCi = source.functionalSubsystemCi,
        description = source.description,
        ufsServiceAvailable = source.ufsServiceAvailable,
        toolsMeta = source.toolsMeta.map { toolMetaToMap(it) },
        neighboursAgentMeta = source.neighboursAgentMeta.map { neighbourMetaToMap(it) },
        toggles = source.toggles.mapValues { it.value as Any },
        canAccessUserInfo = source.canAccessUserInfo
    )

    @Suppress("UNCHECKED_CAST")
    private fun toolMetaToMap(toolMeta: ToolMeta): Map<String, Any> =
        ObjectMappers.MAPPER.convertValue(toolMeta, Map::class.java) as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    private fun neighbourMetaToMap(neighbourMeta: NeighbourAgentMeta): Map<String, Any> =
        ObjectMappers.MAPPER.convertValue(neighbourMeta, Map::class.java) as Map<String, Any>
}
