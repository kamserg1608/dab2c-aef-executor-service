@file:Suppress("UndocumentedPublicFunction", "TooManyFunctions", "LongMethod")

package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import ru.sbrf.dab2c.executor.clients.converter.ProtoTypeConverters
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AnyExampleInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AnyExampleOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AudioSettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaChatSettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunction
import ru.sbrf.dab2c.executor.clients.giga.agent.model.InitialContextOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Performers
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.StubSoundsOutput
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.anyExample
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audioSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.disableInterruption
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.filterSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.firstSpeaker
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.function
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCall
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionRanker
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionRegistry
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionSoundRule
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaChatSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.initialContext
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.input
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.lockFunctionExecution
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.message
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.output
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.pair
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.params
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.requestContentSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.responseContentSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.settings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.stubSounds
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.triggerFunction
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.triggerGeneration
import ru.sbrf.dab2c.executor.domain.voice.FunctionOptions
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.clients.giga.agent.model.DisableInterruption as ApiDisableInterruption
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FilterSettings as ApiFilterSettings
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FirstSpeaker as ApiFirstSpeaker
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionRanker as ApiFunctionRanker
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionRegistry as ApiFunctionRegistry
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionResult as ApiFunctionResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionSoundRule as ApiFunctionSoundRule
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Input as ApiInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.LockFunctionExecution as ApiLockFunctionExecution
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Message as ApiMessage
import ru.sbrf.dab2c.executor.clients.giga.agent.model.OutputOutput as ApiOutputOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.RequestContentSettings as ApiRequestContentSettings
import ru.sbrf.dab2c.executor.clients.giga.agent.model.ResponseContentSettings as ApiResponseContentSettings
import ru.sbrf.dab2c.executor.clients.giga.agent.model.TriggerFunction as ApiTriggerFunction
import ru.sbrf.dab2c.executor.clients.giga.agent.model.TriggerGeneration as ApiTriggerGeneration
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.AnyExample as ProtoAnyExample
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.AudioSettings as ProtoAudioSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.DisableInterruption as ProtoDisableInterruption
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FilterSettings as ProtoFilterSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FirstSpeaker as ProtoFirstSpeaker
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Function as ProtoFunction
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionRanker as ProtoFunctionRanker
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionRegistry as ProtoFunctionRegistry
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionResult as ProtoFunctionResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionSoundRule as ProtoFunctionSoundRule
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaChatSettings as ProtoGigaChatSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.InitialContext as ProtoInitialContext
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Input as ProtoInput
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.LockFunctionExecution as ProtoLockFunctionExecution
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Message as ProtoMessage
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Output as ProtoOutput
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.RequestContentSettings as ProtoRequestContentSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.ResponseContentSettings as ProtoResponseContentSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings as ProtoSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.StubSounds as ProtoStubSounds
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.TriggerFunction as ProtoTriggerFunction
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.TriggerGeneration as ProtoTriggerGeneration

/**
 * Maps GigaVoice Agent OpenAPI response models ("Output" types) and `Performers` to proto
 * and domain types. Used by the client to interpret incoming HTTP responses.
 */
object GigaVoiceApiToProtoMapper {

    fun toProtoSettings(source: SettingsOutput): ProtoSettings = settings {
        voiceCallId = source.voiceCallId
        audio = toProtoAudioSettings(source.audio)
        source.gigachat?.let { gigachat = toProtoGigaChatSettings(it) }
        source.context?.let { context = toProtoInitialContext(it) }
        disableVad = source.disableVad ?: false
        enableTranscribeInput = source.enableTranscribeInput ?: false
        flags.addAll(source.flags ?: emptyList())
        outputModalities = source.outputModalities?.let { ProtoSettings.OutputModalities.forNumber(it) }
            ?: ProtoSettings.OutputModalities.MODALITIES_UNSPECIFIED
        mode = source.mode?.let { ProtoSettings.Mode.forNumber(it) }
            ?: ProtoSettings.Mode.MODE_UNSPECIFIED
        source.firstSpeaker?.let { firstSpeaker = toProtoFirstSpeaker(it) }
        enableDenoiser = source.enableDenoiser ?: false
        enablePrefetch = source.enablePrefetch ?: false
        enablePersonIdentity = source.enablePersonIdentity ?: false
        enableWhisper = source.enableWhisper ?: false
        enableEmotion = source.enableEmotion ?: false
        enableTranscribeSilencePhrases = source.enableTranscribeSilencePhrases ?: false
        source.disableInterruption?.let { disableInterruption = toProtoDisableInterruption(it) }
    }

    fun toProtoFunctionResult(source: ApiFunctionResult): ProtoFunctionResult = functionResult {
        content = source.content
        source.functionName?.let { functionName = it }
    }

    private fun toProtoStubSounds(source: StubSoundsOutput): ProtoStubSounds = stubSounds {
        source.triggerGeneration?.let { triggerGeneration = toProtoTriggerGeneration(it) }
        source.triggerFunction?.let { triggerFunction = toProtoTriggerFunction(it) }
        sounds.addAll(source.sounds)
    }

    private fun toProtoMessage(source: ApiMessage): ProtoMessage = message {
        role = source.role
        content = source.content
        source.functionCall?.let { fc ->
            functionCall = functionCall {
                name = fc.name
                arguments = fc.arguments.orEmpty()
            }
        }
        source.functionName?.let { functionName = it }
        source.functionsStateId?.let { functionsStateId = it }
        attachments.addAll(source.attachments ?: emptyList())
        inlineData.putAll(source.inlineData ?: emptyMap())
        functions.addAll(source.functions?.map { toProtoFunction(it) } ?: emptyList())
    }

    fun toDomainPerformers(source: Performers): FunctionPerformers = FunctionPerformers(
        functions = source.functions.associate { it.name to toDomainFunctionOptions(it) }
    )

    private fun toDomainFunctionOptions(source: GigaVoiceFunction): FunctionOptions = FunctionOptions(
        isBackendFunction = source.isBackendFunction
    )

    private fun toProtoAudioSettings(source: AudioSettingsOutput?): ProtoAudioSettings = audioSettings {
        source?.input?.let { input = toProtoInput(it) }
        source?.output?.let { output = toProtoOutput(it) }
    }

    private fun toProtoInput(source: ApiInput): ProtoInput = input {
        source.model?.let { model = it }
        audioEncoding = source.audioEncoding?.let { ProtoInput.AudioEncoding.forNumber(it) }
            ?: ProtoInput.AudioEncoding.AUDIO_ENCODING_UNSPECIFIED
        source.sampleRate?.let { sampleRate = it }
        silencePhrases.addAll(source.silencePhrases ?: emptyList())
        TypeConverters.stringToDuration(source.silencePhrasesTimeout)
            ?.let { silencePhrasesTimeout = ProtoTypeConverters.kotlinDurationToProtoDuration(it) }
        TypeConverters.stringToDuration(source.silenceTimeout)
            ?.let { silenceTimeout = ProtoTypeConverters.kotlinDurationToProtoDuration(it) }
        stopPhrases.addAll(source.stopPhrases ?: emptyList())
        ignorePhrases.addAll(source.ignorePhrases ?: emptyList())
    }

    private fun toProtoOutput(source: ApiOutputOutput): ProtoOutput = output {
        source.voice?.let { voice = it }
        audioEncoding = source.audioEncoding?.let { ProtoOutput.AudioEncoding.forNumber(it) }
            ?: ProtoOutput.AudioEncoding.AUDIO_ENCODING_UNSPECIFIED
        source.stubSounds?.let { stubSounds = toProtoStubSounds(it) }
        speed = source.speed?.let { ProtoOutput.Speed.forNumber(it) }
            ?: ProtoOutput.Speed.SPEED_UNSPECIFIED
    }

    private fun toProtoTriggerGeneration(source: ApiTriggerGeneration): ProtoTriggerGeneration = triggerGeneration {
        TypeConverters.stringToDuration(source.timeout)
            ?.let { timeout = ProtoTypeConverters.kotlinDurationToProtoDuration(it) }
        enable = source.enable ?: false
    }

    private fun toProtoTriggerFunction(source: ApiTriggerFunction): ProtoTriggerFunction = triggerFunction {
        enable = source.enable
        mode = source.mode?.let { ProtoTriggerFunction.Mode.forNumber(it) }
            ?: ProtoTriggerFunction.Mode.MODE_UNSPECIFIED
        functionNames.addAll(source.functionNames ?: emptyList())
        rules.addAll(source.rules?.map { toProtoFunctionSoundRule(it) } ?: emptyList())
    }

    private fun toProtoFunctionSoundRule(source: ApiFunctionSoundRule): ProtoFunctionSoundRule = functionSoundRule {
        functionNames.addAll(source.functionNames ?: emptyList())
        sounds.addAll(source.sounds ?: emptyList())
    }

    private fun toProtoDisableInterruption(source: ApiDisableInterruption): ProtoDisableInterruption =
        disableInterruption {
            functions.addAll(source.functions?.map { toProtoLockFunctionExecution(it) } ?: emptyList())
        }

    private fun toProtoLockFunctionExecution(source: ApiLockFunctionExecution): ProtoLockFunctionExecution =
        lockFunctionExecution {
            name = source.name
            onExecution = source.onExecution ?: false
            afterResult = source.afterResult ?: false
        }

    private fun toProtoGigaChatSettings(source: GigaChatSettingsOutput): ProtoGigaChatSettings = gigaChatSettings {
        source.model?.let { model = it }
        TypeConverters.bigDecimalToFloat(source.temperature)?.let { temperature = it }
        TypeConverters.bigDecimalToFloat(source.topP)?.let { topP = it }
        TypeConverters.bigDecimalToFloat(source.repetitionPenalty)?.let { repetitionPenalty = it }
        TypeConverters.bigDecimalToFloat(source.updateInterval)?.let { updateInterval = it }
        source.profanityCheck?.let { profanityCheck = it }
        filtersSettings.putAll(
            source.filtersSettings?.mapValues { (_, v) -> toProtoFilterSettings(v) } ?: emptyMap()
        )
        functions.addAll(source.functions?.map { toProtoFunctionFromOutput(it) } ?: emptyList())
        source.functionRegistry?.let { functionRegistry = toProtoFunctionRegistry(it) }
        filterStubPhrases.addAll(source.filterStubPhrases ?: emptyList())
        source.currentTime?.let { currentTime = it }
        source.functionRanker?.let { functionRanker = toProtoFunctionRanker(it) }
        source.preset?.let { preset = it }
    }

    private fun toProtoFilterSettings(source: ApiFilterSettings): ProtoFilterSettings = filterSettings {
        source.requestContent?.let { requestContent = toProtoRequestContentSettings(it) }
        source.responseContent?.let { responseContent = toProtoResponseContentSettings(it) }
    }

    private fun toProtoRequestContentSettings(source: ApiRequestContentSettings): ProtoRequestContentSettings =
        requestContentSettings {
            source.neuro?.let { neuro = it }
            source.blacklist?.let { blacklist = it }
            source.whitelist?.let { whitelist = it }
        }

    private fun toProtoResponseContentSettings(source: ApiResponseContentSettings): ProtoResponseContentSettings =
        responseContentSettings {
            source.blacklist?.let { blacklist = it }
        }

    private fun toProtoFunction(source: FunctionInput): ProtoFunction = function {
        name = source.name
        source.description?.let { description = it }
        source.parameters?.let { parameters = it }
        fewShotExamples.addAll(source.fewShotExamples?.map { toProtoAnyExample(it) } ?: emptyList())
        source.returnParameters?.let { returnParameters = it }
    }

    private fun toProtoFunctionFromOutput(source: FunctionOutput): ProtoFunction = function {
        name = source.name
        source.description?.let { description = it }
        source.parameters?.let { parameters = it }
        fewShotExamples.addAll(source.fewShotExamples?.map { toProtoAnyExampleFromOutput(it) } ?: emptyList())
        source.returnParameters?.let { returnParameters = it }
    }

    private fun toProtoAnyExample(source: AnyExampleInput): ProtoAnyExample = anyExample {
        request = source.request
        params = params {
            pairs.addAll(
                source.params.pairs.map {
                    pair {
                        key = it.key
                        value = it.value
                    }
                }
            )
        }
    }

    private fun toProtoAnyExampleFromOutput(source: AnyExampleOutput): ProtoAnyExample = anyExample {
        request = source.request
        params = params {
            pairs.addAll(
                source.params.pairs.map {
                    pair {
                        key = it.key
                        value = it.value
                    }
                }
            )
        }
    }

    private fun toProtoFunctionRegistry(source: ApiFunctionRegistry): ProtoFunctionRegistry = functionRegistry {
        source.profile?.let { profile = it }
        labels.addAll(source.labels ?: emptyList())
        source.abFlags?.let { abFlags = it }
    }

    private fun toProtoFunctionRanker(source: ApiFunctionRanker): ProtoFunctionRanker = functionRanker {
        source.enabled?.let { enabled = it }
        source.topN?.let { topN = it }
        source.embedderModel?.let { embedderModel = it }
        ignoredFunctions.addAll(source.ignoredFunctions ?: emptyList())
    }

    private fun toProtoInitialContext(source: InitialContextOutput): ProtoInitialContext = initialContext {
        messages.addAll(source.messages.map { toProtoMessage(it) })
    }

    private fun toProtoFirstSpeaker(source: ApiFirstSpeaker): ProtoFirstSpeaker = firstSpeaker {
        source.type?.let { type = it }
        source.lockFirstIn?.let { lockFirstIn = it }
    }
}
