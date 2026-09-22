package ru.sbrf.dab2c.executor.clients.configurator.mapper

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
import ru.sbrf.dab2c.executor.clients.configurator.model.Settings
import ru.sbrf.dab2c.executor.clients.configurator.model.StubSounds
import ru.sbrf.dab2c.executor.clients.configurator.model.TriggerFunction
import ru.sbrf.dab2c.executor.domain.configuration.AnyExample as DomainAnyExample
import ru.sbrf.dab2c.executor.domain.configuration.AudioOutputSettings as DomainAudioOutputSettings
import ru.sbrf.dab2c.executor.domain.configuration.AudioSettings as DomainAudioSettings
import ru.sbrf.dab2c.executor.domain.configuration.DisableInterruptionSettings as DomainDisableInterruptionSettings
import ru.sbrf.dab2c.executor.domain.configuration.Function as DomainFunction
import ru.sbrf.dab2c.executor.domain.configuration.FunctionList as DomainFunctionList
import ru.sbrf.dab2c.executor.domain.configuration.FunctionRanker as DomainFunctionRanker
import ru.sbrf.dab2c.executor.domain.configuration.FunctionSoundRule as DomainFunctionSoundRule
import ru.sbrf.dab2c.executor.domain.configuration.GigachatSettings as DomainGigachatSettings
import ru.sbrf.dab2c.executor.domain.configuration.LockFunctionExecution as DomainLockFunctionExecution
import ru.sbrf.dab2c.executor.domain.configuration.Params as DomainParams
import ru.sbrf.dab2c.executor.domain.configuration.Settings as DomainSettings
import ru.sbrf.dab2c.executor.domain.configuration.StubSounds as DomainStubSounds
import ru.sbrf.dab2c.executor.domain.configuration.TriggerFunction as DomainTriggerFunction

/**
 * Maps Configurator function list responses to domain models.
 *
 * Every field of the Configurator contract is optional, so an absent field
 * falls back to its domain default instead of failing the mapping.
 */
object FunctionListResponseMapper {

    /** Converts a function list response to its domain representation. */
    fun toDomain(source: FunctionListResponse): DomainFunctionList =
        DomainFunctionList(
            settings = source.settings?.let(::toDomain) ?: DomainSettings()
        )

    private fun toDomain(source: Settings): DomainSettings =
        DomainSettings(
            gigachat = source.gigachat?.let(::toDomain) ?: DomainGigachatSettings(),
            audio = source.audio?.let(::toDomain) ?: DomainAudioSettings(),
            disableInterruption = source.disableInterruption?.let(::toDomain)
                ?: DomainDisableInterruptionSettings()
        )

    private fun toDomain(source: GigachatSettings): DomainGigachatSettings =
        DomainGigachatSettings(
            functions = source.functions?.map(::toDomain).orEmpty(),
            functionRanker = source.functionRanker?.let(::toDomain) ?: DomainFunctionRanker()
        )

    private fun toDomain(source: FunctionRanker): DomainFunctionRanker =
        DomainFunctionRanker(
            ignoredFunctions = source.ignoredFunctions.orEmpty()
        )

    private fun toDomain(source: Function): DomainFunction =
        DomainFunction(
            name = source.name.orEmpty(),
            description = source.description.orEmpty(),
            parameters = source.parameters.orEmpty(),
            fewShotExamples = source.fewShotExamples?.map(::toDomain).orEmpty(),
            returnParameters = source.returnParameters.orEmpty()
        )

    private fun toDomain(source: AnyExample): DomainAnyExample =
        DomainAnyExample(
            request = source.request.orEmpty(),
            params = DomainParams(
                pairs = source.params.orEmpty().map { (name, value) -> name to value }
            )
        )

    private fun toDomain(source: AudioSettings): DomainAudioSettings =
        DomainAudioSettings(
            output = source.output?.let(::toDomain) ?: DomainAudioOutputSettings()
        )

    private fun toDomain(source: AudioOutputSettings): DomainAudioOutputSettings =
        DomainAudioOutputSettings(
            stubSounds = source.stubSounds?.let(::toDomain) ?: DomainStubSounds()
        )

    private fun toDomain(source: StubSounds): DomainStubSounds =
        DomainStubSounds(
            triggerFunction = source.triggerFunction?.let(::toDomain) ?: DomainTriggerFunction()
        )

    private fun toDomain(source: TriggerFunction): DomainTriggerFunction =
        DomainTriggerFunction(
            functionNames = source.functionNames.orEmpty(),
            rules = source.rules?.map(::toDomain).orEmpty()
        )

    private fun toDomain(source: FunctionSoundRule): DomainFunctionSoundRule =
        DomainFunctionSoundRule(
            functionNames = source.functionNames.orEmpty(),
            sounds = source.sounds.orEmpty()
        )

    private fun toDomain(source: DisableInterruptionSettings): DomainDisableInterruptionSettings =
        DomainDisableInterruptionSettings(
            functions = source.functions?.map(::toDomain).orEmpty()
        )

    private fun toDomain(source: LockFunctionExecution): DomainLockFunctionExecution =
        DomainLockFunctionExecution(
            name = source.name.orEmpty(),
            onExecution = source.onExecution ?: false,
            afterResult = source.afterResult ?: false
        )
}
