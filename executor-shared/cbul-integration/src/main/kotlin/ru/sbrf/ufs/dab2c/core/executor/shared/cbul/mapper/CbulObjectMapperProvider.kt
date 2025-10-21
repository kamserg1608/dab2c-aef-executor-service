package ru.sbrf.ufs.dab2c.core.executor.shared.cbul.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import ru.sbrf.ufs.platform.core.json.DefaultJsonMapperLocator

/**
 * Class for providing custom [ObjectMapper] for CBUL.
 */
class CbulObjectMapperProvider {

    /**
     * Provides [ObjectMapper].
     */
    fun getMapper(): ObjectMapper = DefaultJsonMapperLocator().mapper
        .registerModules(KotlinModule.Builder().build())
        .registerModules(JavaTimeModule())
}
