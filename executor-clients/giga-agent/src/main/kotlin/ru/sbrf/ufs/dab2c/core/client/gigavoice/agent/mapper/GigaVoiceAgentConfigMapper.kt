package ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AgentConfig
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.configuration.NeighbourAgentMeta
import ru.sbrf.dab2c.executor.domain.configuration.ToolMeta

/**
 * Manual mapper for converting domain AgentConfiguration to API AgentConfig.
 * Uses Jackson ObjectMapper for converting typed domain models to untyped Map.
 */
object GigaVoiceAgentConfigMapper {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

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
        objectMapper.convertValue(toolMeta, Map::class.java) as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    private fun neighbourMetaToMap(neighbourMeta: NeighbourAgentMeta): Map<String, Any> =
        objectMapper.convertValue(neighbourMeta, Map::class.java) as Map<String, Any>
}
