package ru.sbrf.dab2c.executor.clients.configurator.mapper

import io.mcarle.konvert.api.Konverter
import ru.sbrf.dab2c.executor.clients.configurator.model.AnyExample
import ru.sbrf.dab2c.executor.clients.configurator.model.AudioOutputSettings
import ru.sbrf.dab2c.executor.clients.configurator.model.AudioSettings
import ru.sbrf.dab2c.executor.clients.configurator.model.DisableInterruptionSettings
import ru.sbrf.dab2c.executor.clients.configurator.model.Function
import ru.sbrf.dab2c.executor.clients.configurator.model.FunctionListResponse
import ru.sbrf.dab2c.executor.clients.configurator.model.FunctionRanker
import ru.sbrf.dab2c.executor.clients.configurator.model.FunctionSoundRule
import ru.sbrf.dab2c.executor.clients.configurator.model.GigachatSettings
import ru.sbrf.dab2c.executor.clients.configurator.model.LockFunctionExecution
import ru.sbrf.dab2c.executor.clients.configurator.model.Pair
import ru.sbrf.dab2c.executor.clients.configurator.model.Params
import ru.sbrf.dab2c.executor.clients.configurator.model.Settings
import ru.sbrf.dab2c.executor.clients.configurator.model.StubSounds
import ru.sbrf.dab2c.executor.clients.configurator.model.TriggerFunction
import kotlin.collections.map
import kotlin.collections.orEmpty
import kotlin.text.orEmpty
import ru.sbrf.dab2c.executor.domain.configuration.AnyExample as DomainAnyExample
import ru.sbrf.dab2c.executor.domain.configuration.AudioOutputSettings as DomainAudioOutputSettings
import ru.sbrf.dab2c.executor.domain.configuration.AudioSettings as DomainAudioSettings
import ru.sbrf.dab2c.executor.domain.configuration.DisableInterruptionSettings as DomainDisableInterruptionSettings
import ru.sbrf.dab2c.executor.domain.configuration.Function as DomainFunction
import ru.sbrf.dab2c.executor.domain.configuration.FunctionListResponse as DomainFunctionListResponse
import ru.sbrf.dab2c.executor.domain.configuration.FunctionRanker as DomainFunctionRanker
import ru.sbrf.dab2c.executor.domain.configuration.FunctionSoundRule as DomainFunctionSoundRule
import ru.sbrf.dab2c.executor.domain.configuration.GigachatSettings as DomainGigachatSettings
import ru.sbrf.dab2c.executor.domain.configuration.LockFunctionExecution as DomainLockFunctionExecution
import ru.sbrf.dab2c.executor.domain.configuration.Pair as DomainPair
import ru.sbrf.dab2c.executor.domain.configuration.Params as DomainParams
import ru.sbrf.dab2c.executor.domain.configuration.Settings as DomainSettings
import ru.sbrf.dab2c.executor.domain.configuration.StubSounds as DomainStubSounds
import ru.sbrf.dab2c.executor.domain.configuration.TriggerFunction as DomainTriggerFunction

/**
 * Mapper for converting EFS adapter function settings models to domain models.
 */
@Konverter
interface FunctionListResponseMapper :
    FunctionListResponseCoreMapper,
    FunctionListResponseAudioMapper,
    FunctionListResponseInterruptionMapper {

    override fun toDomain(source: Function): DomainFunction =
        DomainFunction(
            name = source.name.orEmpty(),
            description = source.description.orEmpty(),
            parameters = source.parameters.orEmpty(),
            fewShotExamples = source.fewShotExamples?.map(::toDomain).orEmpty(),
            returnParameters = source.returnParameters.orEmpty()
        )

    /** Provides singleton instance of the mapper. */
    companion object {
        val INSTANCE: FunctionListResponseMapper get() = FunctionListResponseMapperImpl
    }
}

/**
 * Core mapper methods for function list response.
 */
interface FunctionListResponseCoreMapper {

    /** Converts FunctionListResponse to domain FunctionListResponse. */
    fun toDomain(source: FunctionListResponse): DomainFunctionListResponse

    /** Converts Settings to domain Settings. */
    fun toDomain(source: Settings): DomainSettings

    /** Converts GigachatSettings to domain GigachatSettings. */
    fun toDomain(source: GigachatSettings): DomainGigachatSettings

    /** Converts FunctionRanker to domain FunctionRanker. */
    fun toDomain(source: FunctionRanker): DomainFunctionRanker

    /** Converts Function to domain Function. */
    fun toDomain(source: Function): DomainFunction

    /** Converts AnyExample to domain AnyExample. */
    fun toDomain(source: AnyExample): DomainAnyExample

    /** Converts Params to domain Params. */
    fun toDomain(source: Params): DomainParams

    /** Converts Pair to domain Pair. */
    fun toDomain(source: Pair): DomainPair
}

/**
 * Mapper methods for audio-related function settings.
 */
interface FunctionListResponseAudioMapper {

    /** Converts AudioSettings to domain AudioSettings. */
    fun toDomain(source: AudioSettings): DomainAudioSettings

    /** Converts AudioOutputSettings to domain AudioOutputSettings. */
    fun toDomain(source: AudioOutputSettings): DomainAudioOutputSettings

    /** Converts StubSounds to domain StubSounds. */
    fun toDomain(source: StubSounds): DomainStubSounds

    /** Converts TriggerFunction to domain TriggerFunction. */
    fun toDomain(source: TriggerFunction): DomainTriggerFunction

    /** Converts FunctionSoundRule to domain FunctionSoundRule. */
    fun toDomain(source: FunctionSoundRule): DomainFunctionSoundRule
}

/**
 * Mapper methods for interruption locking settings.
 */
interface FunctionListResponseInterruptionMapper {

    /** Converts DisableInterruptionSettings to domain DisableInterruptionSettings. */
    fun toDomain(source: DisableInterruptionSettings): DomainDisableInterruptionSettings

    /** Converts LockFunctionExecution to domain LockFunctionExecution. */
    fun toDomain(source: LockFunctionExecution): DomainLockFunctionExecution
}
