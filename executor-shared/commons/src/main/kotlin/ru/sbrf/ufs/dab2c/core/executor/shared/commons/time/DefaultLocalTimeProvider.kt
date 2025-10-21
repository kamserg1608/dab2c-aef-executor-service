package ru.sbrf.ufs.dab2c.core.executor.shared.commons.time

import java.time.LocalDateTime

/**
 * Default [LocalTimeProvider].
 */
class DefaultLocalTimeProvider : LocalTimeProvider {

    override fun now(): LocalDateTime = LocalDateTime.now()

    override fun currentTimeMillis() = System.currentTimeMillis()
}
