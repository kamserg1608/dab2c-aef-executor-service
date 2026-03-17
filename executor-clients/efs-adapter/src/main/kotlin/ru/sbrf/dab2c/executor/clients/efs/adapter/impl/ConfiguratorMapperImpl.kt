package ru.sbrf.dab2c.executor.clients.efs.adapter.impl

import ru.sbrf.dab2c.executor.clients.efs.adapter.mapper.ConfiguratorMapper
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.SessionConfig
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon

/**
 * Manual mapper implementation that uppercases the channel field.
 */
class ConfiguratorMapperImpl : ConfiguratorMapper {

    override fun toDomain(source: SessionConfig): DaSessionCommon =
        DaSessionCommon(
            block = source.block,
            channel = source.channel?.uppercase().orEmpty(),
            surface = source.surface,
            platform = source.platform,
            sdkVersion = source.sdkVersion,
            entryPoint = source.entryPoint,
            appVersion = source.appVersion,
            channelVersion = source.channelVersion,
            timeZone = source.timeZone
        )
}
