package ru.sbrf.ufs.dab2c.core.executor.shared.rest.mapper.api

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Service for providing [ObjectMapper].
 */
interface RestObjectMapperProvider {

    /**
     * Provides customized [ObjectMapper].
     */
    fun getMapper(): ObjectMapper
}
