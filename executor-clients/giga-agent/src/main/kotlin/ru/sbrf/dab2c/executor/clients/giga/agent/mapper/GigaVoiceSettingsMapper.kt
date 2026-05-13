@file:Suppress("UndocumentedPublicFunction", "TooManyFunctions", "LongMethod")

package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import ru.sbrf.dab2c.executor.clients.converter.ProtoTypeConverters
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AnyExampleInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AnyExampleOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AudioSettingsInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AudioSettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaChatSettingsInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaChatSettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunction
import ru.sbrf.dab2c.executor.clients.giga.agent.model.InitialContextInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.InitialContextOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Performers
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.StubSoundsInput
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
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCall as ApiFunctionCall
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCalling as ApiFunctionCalling
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionRanker as ApiFunctionRanker
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionRegistry as ApiFunctionRegistry
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionResult as ApiFunctionResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionSoundRule as ApiFunctionSoundRule
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Input as ApiInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.LockFunctionExecution as ApiLockFunctionExecution
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Message as ApiMessage
import ru.sbrf.dab2c.executor.clients.giga.agent.model.OutputInput as ApiOutputInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.OutputOutput as ApiOutputOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Pair as ApiPair
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Params as ApiParams
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
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling as ProtoFunctionCalling
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
 * Mapper between proto voice messages and GigaVoice Agent OpenAPI models.
 */
object GigaVoiceSettingsMapper {

    // === proto.Settings -> SettingsInput ===
    fun toApiSettingsInput(source: ProtoSettings): SettingsInput = SettingsInput(
        audio = toApiAudioSettings(source.audio),
        voiceCallId = source.voiceCallId,
        gigachat = if (source.hasGigachat()) toApiGigaChatSettings(source.gigachat) else null,
        context = if (source.hasContext()) toApiInitialContext(source.context) else null,
        disableVad = source.disableVad,
        enableTranscribeInput = source.enableTranscribeInput,
        flags = source.flagsList.takeIf { it.isNotEmpty() },
        outputModalities = source.outputModalities.number,
        mode = source.mode.number,
        firstSpeaker = if (source.hasFirstSpeaker()) toApiFirstSpeaker(source.firstSpeaker) else null,
        enableDenoiser = source.enableDenoiser,
        enablePrefetch = source.enablePrefetch,
        enablePersonIdentity = source.enablePersonIdentity,
        enableWhisper = source.enableWhisper,
        enableEmotion = source.enableEmotion,
        enableTranscribeSilencePhrases = source.enableTranscribeSilencePhrases,
        disableInterruption = if (source.hasDisableInterruption()) {
            toApiDisableInterruption(source.disableInterruption)
        } else {
            null
        },
    )

    // === SettingsOutput -> proto.Settings ===
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

    // === Performers -> FunctionPerformers ===
    fun toDomainPerformers(source: Performers): FunctionPerformers = FunctionPerformers(
        functions = source.functions.associate { it.name to toDomainFunctionOptions(it) }
    )

    fun toDomainFunctionOptions(source: GigaVoiceFunction): FunctionOptions = FunctionOptions(
        isBackendFunction = source.isBackendFunction
    )

    // === Function calling ===

    fun toApiFunctionCalling(source: ProtoFunctionCalling): ApiFunctionCalling = ApiFunctionCalling(
        functionCall = ApiFunctionCall(
            name = source.functionCall.name,
            arguments = source.functionCall.arguments
        ),
        timestamp = source.timestamp.toInt()
    )

    fun toProtoFunctionResult(source: ApiFunctionResult): ProtoFunctionResult = functionResult {
        content = source.content
        source.functionName?.let { functionName = it }
    }

    // === Audio settings ===

    private fun toApiAudioSettings(source: ProtoAudioSettings): AudioSettingsInput = AudioSettingsInput(
        input = if (source.hasInput()) toApiInput(source.input) else null,
        output = if (source.hasOutput()) toApiOutput(source.output) else null
    )

    private fun toProtoAudioSettings(source: AudioSettingsOutput?): ProtoAudioSettings = audioSettings {
        source?.input?.let { input = toProtoInput(it) }
        source?.output?.let { output = toProtoOutput(it) }
    }

    private fun toApiInput(source: ProtoInput): ApiInput = ApiInput(
        model = source.model.takeIf { it.isNotEmpty() },
        audioEncoding = source.audioEncoding.number,
        sampleRate = if (source.hasSampleRate()) source.sampleRate else null,
        silencePhrases = source.silencePhrasesList.takeIf { it.isNotEmpty() },
        silencePhrasesTimeout = if (source.hasSilencePhrasesTimeout()) {
            TypeConverters.durationToString(
                ProtoTypeConverters.protoDurationToKotlinDuration(source.silencePhrasesTimeout)
            )
        } else {
            null
        },
        silenceTimeout = if (source.hasSilenceTimeout()) {
            TypeConverters.durationToString(
                ProtoTypeConverters.protoDurationToKotlinDuration(source.silenceTimeout)
            )
        } else {
            null
        },
        stopPhrases = source.stopPhrasesList.takeIf { it.isNotEmpty() },
        ignorePhrases = source.ignorePhrasesList.takeIf { it.isNotEmpty() }
    )

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

    private fun toApiOutput(source: ProtoOutput): ApiOutputInput = ApiOutputInput(
        voice = source.voice.takeIf { it.isNotEmpty() },
        audioEncoding = source.audioEncoding.number,
        stubSounds = if (source.hasStubSounds()) toApiStubSoundsInput(source.stubSounds) else null,
        speed = source.speed.number,
    )

    private fun toProtoOutput(source: ApiOutputOutput): ProtoOutput = output {
        source.voice?.let { voice = it }
        audioEncoding = source.audioEncoding?.let { ProtoOutput.AudioEncoding.forNumber(it) }
            ?: ProtoOutput.AudioEncoding.AUDIO_ENCODING_UNSPECIFIED
        source.stubSounds?.let { stubSounds = toProtoStubSounds(it) }
        speed = source.speed?.let { ProtoOutput.Speed.forNumber(it) }
            ?: ProtoOutput.Speed.SPEED_UNSPECIFIED
    }

    // === Stub sounds ===

    fun toApiStubSoundsInput(source: ProtoStubSounds): StubSoundsInput = StubSoundsInput(
        triggerGeneration = if (source.hasTriggerGeneration()) {
            toApiTriggerGeneration(source.triggerGeneration)
        } else {
            null
        },
        triggerFunction = if (source.hasTriggerFunction()) {
            toApiTriggerFunction(source.triggerFunction)
        } else {
            null
        },
        sounds = source.soundsList
    )

    fun toProtoStubSounds(source: StubSoundsOutput): ProtoStubSounds = stubSounds {
        source.triggerGeneration?.let { triggerGeneration = toProtoTriggerGeneration(it) }
        source.triggerFunction?.let { triggerFunction = toProtoTriggerFunction(it) }
        sounds.addAll(source.sounds)
    }

    private fun toApiTriggerGeneration(source: ProtoTriggerGeneration): ApiTriggerGeneration = ApiTriggerGeneration(
        timeout = if (source.hasTimeout()) {
            TypeConverters.durationToString(ProtoTypeConverters.protoDurationToKotlinDuration(source.timeout))
        } else {
            null
        },
        enable = source.enable
    )

    private fun toProtoTriggerGeneration(source: ApiTriggerGeneration): ProtoTriggerGeneration = triggerGeneration {
        TypeConverters.stringToDuration(source.timeout)
            ?.let { timeout = ProtoTypeConverters.kotlinDurationToProtoDuration(it) }
        enable = source.enable ?: false
    }

    private fun toApiTriggerFunction(source: ProtoTriggerFunction): ApiTriggerFunction = ApiTriggerFunction(
        enable = source.enable,
        mode = source.mode.number,
        functionNames = source.functionNamesList,
        rules = source.rulesList.map { toApiFunctionSoundRule(it) },
    )

    private fun toProtoTriggerFunction(source: ApiTriggerFunction): ProtoTriggerFunction = triggerFunction {
        enable = source.enable
        mode = source.mode?.let { ProtoTriggerFunction.Mode.forNumber(it) }
            ?: ProtoTriggerFunction.Mode.MODE_UNSPECIFIED
        functionNames.addAll(source.functionNames ?: emptyList())
        rules.addAll(source.rules?.map { toProtoFunctionSoundRule(it) } ?: emptyList())
    }

    private fun toApiFunctionSoundRule(source: ProtoFunctionSoundRule): ApiFunctionSoundRule = ApiFunctionSoundRule(
        functionNames = source.functionNamesList,
        sounds = source.soundsList,
    )

    private fun toProtoFunctionSoundRule(source: ApiFunctionSoundRule): ProtoFunctionSoundRule = functionSoundRule {
        functionNames.addAll(source.functionNames ?: emptyList())
        sounds.addAll(source.sounds ?: emptyList())
    }

    // === Disable interruption ===

    private fun toApiDisableInterruption(source: ProtoDisableInterruption): ApiDisableInterruption =
        ApiDisableInterruption(
            functions = source.functionsList.map { toApiLockFunctionExecution(it) },
        )

    private fun toProtoDisableInterruption(source: ApiDisableInterruption): ProtoDisableInterruption =
        disableInterruption {
            functions.addAll(source.functions?.map { toProtoLockFunctionExecution(it) } ?: emptyList())
        }

    private fun toApiLockFunctionExecution(source: ProtoLockFunctionExecution): ApiLockFunctionExecution =
        ApiLockFunctionExecution(
            name = source.name,
            onExecution = source.onExecution,
            afterResult = source.afterResult,
        )

    private fun toProtoLockFunctionExecution(source: ApiLockFunctionExecution): ProtoLockFunctionExecution =
        lockFunctionExecution {
            name = source.name
            onExecution = source.onExecution ?: false
            afterResult = source.afterResult ?: false
        }

    // === GigaChat settings ===

    private fun toApiGigaChatSettings(source: ProtoGigaChatSettings): GigaChatSettingsInput = GigaChatSettingsInput(
        model = source.model.takeIf { it.isNotEmpty() },
        temperature = if (source.hasTemperature()) TypeConverters.floatToBigDecimal(source.temperature) else null,
        topP = if (source.hasTopP()) TypeConverters.floatToBigDecimal(source.topP) else null,
        repetitionPenalty = if (source.hasRepetitionPenalty()) {
            TypeConverters.floatToBigDecimal(source.repetitionPenalty)
        } else {
            null
        },
        updateInterval = if (source.hasUpdateInterval()) {
            TypeConverters.floatToBigDecimal(source.updateInterval)
        } else {
            null
        },
        profanityCheck = if (source.hasProfanityCheck()) source.profanityCheck else null,
        filtersSettings = source.filtersSettingsMap
            .mapValues { (_, v) -> toApiFilterSettings(v) }
            .takeIf { it.isNotEmpty() },
        functions = source.functionsList.map { toApiFunction(it) },
        functionRegistry = if (source.hasFunctionRegistry()) toApiFunctionRegistry(source.functionRegistry) else null,
        filterStubPhrases = source.filterStubPhrasesList.takeIf { it.isNotEmpty() },
        currentTime = if (source.hasCurrentTime()) source.currentTime else null,
        functionRanker = if (source.hasFunctionRanker()) toApiFunctionRanker(source.functionRanker) else null,
        preset = if (source.hasPreset()) source.preset else null,
    )

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

    private fun toApiFilterSettings(source: ProtoFilterSettings): ApiFilterSettings = ApiFilterSettings(
        requestContent = if (source.hasRequestContent()) toApiRequestContentSettings(source.requestContent) else null,
        responseContent = if (source.hasResponseContent()) {
            toApiResponseContentSettings(source.responseContent)
        } else {
            null
        }
    )

    private fun toProtoFilterSettings(source: ApiFilterSettings): ProtoFilterSettings = filterSettings {
        source.requestContent?.let { requestContent = toProtoRequestContentSettings(it) }
        source.responseContent?.let { responseContent = toProtoResponseContentSettings(it) }
    }

    private fun toApiRequestContentSettings(source: ProtoRequestContentSettings): ApiRequestContentSettings =
        ApiRequestContentSettings(
            neuro = if (source.hasNeuro()) source.neuro else null,
            blacklist = if (source.hasBlacklist()) source.blacklist else null,
            whitelist = if (source.hasWhitelist()) source.whitelist else null,
        )

    private fun toProtoRequestContentSettings(source: ApiRequestContentSettings): ProtoRequestContentSettings =
        requestContentSettings {
            source.neuro?.let { neuro = it }
            source.blacklist?.let { blacklist = it }
            source.whitelist?.let { whitelist = it }
        }

    private fun toApiResponseContentSettings(source: ProtoResponseContentSettings): ApiResponseContentSettings =
        ApiResponseContentSettings(
            blacklist = if (source.hasBlacklist()) source.blacklist else null,
        )

    private fun toProtoResponseContentSettings(source: ApiResponseContentSettings): ProtoResponseContentSettings =
        responseContentSettings {
            source.blacklist?.let { blacklist = it }
        }

    private fun toApiFunction(source: ProtoFunction): FunctionInput = FunctionInput(
        name = source.name,
        description = source.description.takeIf { it.isNotEmpty() },
        parameters = source.parameters.takeIf { it.isNotEmpty() },
        fewShotExamples = source.fewShotExamplesList.map { toApiAnyExampleInput(it) },
        returnParameters = source.returnParameters.takeIf { it.isNotEmpty() },
    )

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

    private fun toApiAnyExampleInput(source: ProtoAnyExample): AnyExampleInput = AnyExampleInput(
        request = source.request,
        params = ApiParams(
            pairs = source.params.pairsList.map { ApiPair(key = it.key, value = it.value) }
        )
    )

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

    private fun toApiFunctionRegistry(source: ProtoFunctionRegistry): ApiFunctionRegistry = ApiFunctionRegistry(
        profile = source.profile.takeIf { it.isNotEmpty() },
        labels = source.labelsList.takeIf { it.isNotEmpty() },
        abFlags = source.abFlags.takeIf { it.isNotEmpty() }
    )

    private fun toProtoFunctionRegistry(source: ApiFunctionRegistry): ProtoFunctionRegistry = functionRegistry {
        source.profile?.let { profile = it }
        labels.addAll(source.labels ?: emptyList())
        source.abFlags?.let { abFlags = it }
    }

    private fun toApiFunctionRanker(source: ProtoFunctionRanker): ApiFunctionRanker = ApiFunctionRanker(
        enabled = if (source.hasEnabled()) source.enabled else null,
        topN = if (source.hasTopN()) source.topN.toInt() else null,
        embedderModel = if (source.hasEmbedderModel()) source.embedderModel else null,
        ignoredFunctions = source.ignoredFunctionsList,
    )

    private fun toProtoFunctionRanker(source: ApiFunctionRanker): ProtoFunctionRanker = functionRanker {
        source.enabled?.let { enabled = it }
        source.topN?.let { topN = it }
        source.embedderModel?.let { embedderModel = it }
        ignoredFunctions.addAll(source.ignoredFunctions ?: emptyList())
    }

    // === Initial context / messages ===

    private fun toApiInitialContext(source: ProtoInitialContext): InitialContextInput = InitialContextInput(
        messages = source.messagesList.map { toApiMessage(it) }
    )

    private fun toProtoInitialContext(source: InitialContextOutput): ProtoInitialContext = initialContext {
        messages.addAll(source.messages.map { toProtoMessage(it) })
    }

    fun toApiMessage(source: ProtoMessage): ApiMessage = ApiMessage(
        role = source.role,
        content = source.content,
        functionCall = if (source.hasFunctionCall()) {
            ApiFunctionCall(name = source.functionCall.name, arguments = source.functionCall.arguments)
        } else {
            null
        },
        functionName = source.functionName.takeIf { it.isNotEmpty() },
        functionsStateId = source.functionsStateId.takeIf { it.isNotEmpty() },
        attachments = source.attachmentsList.takeIf { it.isNotEmpty() },
        inlineData = source.inlineDataMap.takeIf { it.isNotEmpty() },
        functions = source.functionsList.takeIf { it.isNotEmpty() }?.map { toApiFunction(it) },
    )

    fun toProtoMessage(source: ApiMessage): ProtoMessage = message {
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

    // === First speaker ===

    private fun toApiFirstSpeaker(source: ProtoFirstSpeaker): ApiFirstSpeaker = ApiFirstSpeaker(
        type = source.type.takeIf { it.isNotEmpty() },
        lockFirstIn = if (source.hasLockFirstIn()) source.lockFirstIn else null,
    )

    private fun toProtoFirstSpeaker(source: ApiFirstSpeaker): ProtoFirstSpeaker = firstSpeaker {
        source.type?.let { type = it }
        source.lockFirstIn?.let { lockFirstIn = it }
    }
}
