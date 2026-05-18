@file:Suppress("UndocumentedPublicFunction", "TooManyFunctions", "LongMethod")

package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import ru.sbrf.dab2c.executor.clients.converter.ProtoTypeConverters
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AnyExampleInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AudioSettingsInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaChatSettingsInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.InitialContextInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.StubSoundsInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.DisableInterruption as ApiDisableInterruption
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FilterSettings as ApiFilterSettings
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FirstSpeaker as ApiFirstSpeaker
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCall as ApiFunctionCall
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCalling as ApiFunctionCalling
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionRanker as ApiFunctionRanker
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionRegistry as ApiFunctionRegistry
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionSoundRule as ApiFunctionSoundRule
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Input as ApiInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.LockFunctionExecution as ApiLockFunctionExecution
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Message as ApiMessage
import ru.sbrf.dab2c.executor.clients.giga.agent.model.OutputInput as ApiOutputInput
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
 * Maps proto voice messages to GigaVoice Agent OpenAPI request models ("Input" types).
 * Used by request builders to construct outgoing HTTP request bodies.
 */
object GigaVoiceProtoToApiMapper {

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

    fun toApiFunctionCalling(source: ProtoFunctionCalling): ApiFunctionCalling = ApiFunctionCalling(
        functionCall = ApiFunctionCall(
            name = source.functionCall.name,
            arguments = source.functionCall.arguments
        ),
        timestamp = source.timestamp.toInt()
    )

    private fun toApiStubSoundsInput(source: ProtoStubSounds): StubSoundsInput = StubSoundsInput(
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

    private fun toApiMessage(source: ProtoMessage): ApiMessage = ApiMessage(
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

    private fun toApiAudioSettings(source: ProtoAudioSettings): AudioSettingsInput = AudioSettingsInput(
        input = if (source.hasInput()) toApiInput(source.input) else null,
        output = if (source.hasOutput()) toApiOutput(source.output) else null
    )

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

    private fun toApiOutput(source: ProtoOutput): ApiOutputInput = ApiOutputInput(
        voice = source.voice.takeIf { it.isNotEmpty() },
        audioEncoding = source.audioEncoding.number,
        stubSounds = if (source.hasStubSounds()) toApiStubSoundsInput(source.stubSounds) else null,
        speed = source.speed.number,
    )

    private fun toApiTriggerGeneration(source: ProtoTriggerGeneration): ApiTriggerGeneration = ApiTriggerGeneration(
        timeout = if (source.hasTimeout()) {
            TypeConverters.durationToString(ProtoTypeConverters.protoDurationToKotlinDuration(source.timeout))
        } else {
            null
        },
        enable = source.enable
    )

    private fun toApiTriggerFunction(source: ProtoTriggerFunction): ApiTriggerFunction = ApiTriggerFunction(
        enable = source.enable,
        mode = source.mode.number,
        functionNames = source.functionNamesList,
        rules = source.rulesList.map { toApiFunctionSoundRule(it) },
    )

    private fun toApiFunctionSoundRule(source: ProtoFunctionSoundRule): ApiFunctionSoundRule = ApiFunctionSoundRule(
        functionNames = source.functionNamesList,
        sounds = source.soundsList,
    )

    private fun toApiDisableInterruption(source: ProtoDisableInterruption): ApiDisableInterruption =
        ApiDisableInterruption(
            functions = source.functionsList.map { toApiLockFunctionExecution(it) },
        )

    private fun toApiLockFunctionExecution(source: ProtoLockFunctionExecution): ApiLockFunctionExecution =
        ApiLockFunctionExecution(
            name = source.name,
            onExecution = source.onExecution,
            afterResult = source.afterResult,
        )

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

    private fun toApiFilterSettings(source: ProtoFilterSettings): ApiFilterSettings = ApiFilterSettings(
        requestContent = if (source.hasRequestContent()) toApiRequestContentSettings(source.requestContent) else null,
        responseContent = if (source.hasResponseContent()) {
            toApiResponseContentSettings(source.responseContent)
        } else {
            null
        }
    )

    private fun toApiRequestContentSettings(source: ProtoRequestContentSettings): ApiRequestContentSettings =
        ApiRequestContentSettings(
            neuro = if (source.hasNeuro()) source.neuro else null,
            blacklist = if (source.hasBlacklist()) source.blacklist else null,
            whitelist = if (source.hasWhitelist()) source.whitelist else null,
        )

    private fun toApiResponseContentSettings(source: ProtoResponseContentSettings): ApiResponseContentSettings =
        ApiResponseContentSettings(
            blacklist = if (source.hasBlacklist()) source.blacklist else null,
        )

    private fun toApiFunction(source: ProtoFunction): FunctionInput = FunctionInput(
        name = source.name,
        description = source.description.takeIf { it.isNotEmpty() },
        parameters = source.parameters.takeIf { it.isNotEmpty() },
        fewShotExamples = source.fewShotExamplesList.map { toApiAnyExampleInput(it) },
        returnParameters = source.returnParameters.takeIf { it.isNotEmpty() },
    )

    private fun toApiAnyExampleInput(source: ProtoAnyExample): AnyExampleInput = AnyExampleInput(
        request = source.request,
        params = ApiParams(
            pairs = source.params.pairsList.map { ApiPair(key = it.key, value = it.value) }
        )
    )

    private fun toApiFunctionRegistry(source: ProtoFunctionRegistry): ApiFunctionRegistry = ApiFunctionRegistry(
        profile = source.profile.takeIf { it.isNotEmpty() },
        labels = source.labelsList.takeIf { it.isNotEmpty() },
        abFlags = source.abFlags.takeIf { it.isNotEmpty() }
    )

    private fun toApiFunctionRanker(source: ProtoFunctionRanker): ApiFunctionRanker = ApiFunctionRanker(
        enabled = if (source.hasEnabled()) source.enabled else null,
        topN = if (source.hasTopN()) source.topN.toInt() else null,
        embedderModel = if (source.hasEmbedderModel()) source.embedderModel else null,
        ignoredFunctions = source.ignoredFunctionsList,
    )

    private fun toApiInitialContext(source: ProtoInitialContext): InitialContextInput = InitialContextInput(
        messages = source.messagesList.map { toApiMessage(it) }
    )

    private fun toApiFirstSpeaker(source: ProtoFirstSpeaker): ApiFirstSpeaker = ApiFirstSpeaker(
        type = source.type.takeIf { it.isNotEmpty() },
        lockFirstIn = if (source.hasLockFirstIn()) source.lockFirstIn else null,
    )
}
