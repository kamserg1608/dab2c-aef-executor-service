@file:Suppress("UndocumentedPublicFunction")

package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import ru.sbrf.dab2c.executor.clients.giga.agent.model.AnyExampleInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AnyExampleOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AudioSettingsInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AudioSettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaChatSettingsInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaChatSettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunction
import ru.sbrf.dab2c.executor.clients.giga.agent.model.InitialContextInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.InitialContextOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Input
import ru.sbrf.dab2c.executor.clients.giga.agent.model.OutputInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.OutputOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Performers
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.StubSoundsInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.StubSoundsOutput
import ru.sbrf.dab2c.executor.domain.voice.AudioEncoding
import ru.sbrf.dab2c.executor.domain.voice.AudioInputSettings
import ru.sbrf.dab2c.executor.domain.voice.AudioOutputSettings
import ru.sbrf.dab2c.executor.domain.voice.AudioSettings
import ru.sbrf.dab2c.executor.domain.voice.FilterSettings
import ru.sbrf.dab2c.executor.domain.voice.FirstSpeaker
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.FunctionDefinition
import ru.sbrf.dab2c.executor.domain.voice.FunctionExample
import ru.sbrf.dab2c.executor.domain.voice.FunctionOptions
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.domain.voice.FunctionRegistry
import ru.sbrf.dab2c.executor.domain.voice.FunctionResultData
import ru.sbrf.dab2c.executor.domain.voice.GigaChatSettings
import ru.sbrf.dab2c.executor.domain.voice.InitialContext
import ru.sbrf.dab2c.executor.domain.voice.Message
import ru.sbrf.dab2c.executor.domain.voice.OutputModalities
import ru.sbrf.dab2c.executor.domain.voice.StubSounds
import ru.sbrf.dab2c.executor.domain.voice.TriggerFunctionMode
import ru.sbrf.dab2c.executor.domain.voice.VoiceMode
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FilterSettings as ApiFilterSettings
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FirstSpeaker as ApiFirstSpeaker
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCalling as ApiFunctionCalling
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionRanker as ApiFunctionRanker
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionRegistry as ApiFunctionRegistry
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Message as ApiMessage
import ru.sbrf.dab2c.executor.clients.giga.agent.model.TriggerFunction as ApiTriggerFunction
import ru.sbrf.dab2c.executor.clients.giga.agent.model.TriggerFunctionMode as ApiTriggerFunctionMode
import ru.sbrf.dab2c.executor.domain.voice.FunctionRanker as DomainFunctionRanker
import ru.sbrf.dab2c.executor.domain.voice.TriggerFunction as DomainTriggerFunction

/**
 * Mapper for converting between domain models and GigaVoice Agent API models.
 */
object GigaVoiceSettingsMapper {

    // === VoiceSettings -> SettingsInput ===
    fun toApiSettingsInput(source: VoiceSettings): SettingsInput = SettingsInput(
        audio = toApiAudioSettings(source.audio),
        voiceCallId = source.voiceCallId,
        gigachat = toApiGigaChatSettings(source.gigachat),
        context = toApiInitialContext(source.context),
        disableVad = source.disableVad,
        enableTranscribeInput = source.enableTranscribeInput,
        flags = source.flags.takeIf { it.isNotEmpty() },
        outputModalities = toApiOutputModalities(source.outputModalities),
        mode = toApiVoiceMode(source.mode),
        firstSpeaker = toApiFirstSpeaker(source.firstSpeaker),
        enableDenoiser = source.enableDenoiser,
        enablePrefetch = source.enablePrefetch,
        enablePersonIdentity = source.enablePersonIdentity,
        enableWhisper = source.enableWhisper,
        enableEmotion = source.enableEmotion
    )

    // === SettingsOutput -> VoiceSettings ===
    fun toDomainSettings(source: SettingsOutput): VoiceSettings = VoiceSettings(
        voiceCallId = source.voiceCallId,
        audio = toDomainAudioSettings(source.audio),
        gigachat = toDomainGigaChatSettings(source.gigachat),
        context = toDomainInitialContext(source.context),
        disableVad = source.disableVad ?: false,
        enableTranscribeInput = source.enableTranscribeInput ?: false,
        flags = source.flags ?: emptyList(),
        outputModalities = toDomainOutputModalities(source.outputModalities),
        mode = toDomainVoiceMode(source.mode),
        firstSpeaker = toDomainFirstSpeaker(source.firstSpeaker),
        enableDenoiser = source.enableDenoiser ?: false,
        enablePrefetch = source.enablePrefetch ?: false,
        enablePersonIdentity = source.enablePersonIdentity ?: false,
        enableWhisper = source.enableWhisper ?: false,
        enableEmotion = source.enableEmotion ?: false
    )

    // === Performers -> FunctionPerformers ===
    fun toDomainPerformers(source: Performers): FunctionPerformers = FunctionPerformers(
        functions = source.functions.associate { it.name to toDomainFunctionOptions(it) }
    )

    fun toDomainFunctionOptions(source: GigaVoiceFunction): FunctionOptions = FunctionOptions(
        isBackendFunction = source.isBackendFunction
    )

    // === Helper methods for enum conversions ===

    fun toApiVoiceMode(mode: VoiceMode): Int =
        mode.value

    fun toDomainVoiceMode(mode: Int?): VoiceMode =
        mode?.let { VoiceMode.entries[it] } ?: VoiceMode.UNSPECIFIED

    fun toApiOutputModalities(modalities: OutputModalities): Int =
        modalities.value

    fun toDomainOutputModalities(modalities: Int?): OutputModalities =
        modalities?.let { OutputModalities.entries[it] } ?: OutputModalities.UNSPECIFIED

    fun toApiAudioEncoding(encoding: AudioEncoding): Int =
        encoding.value

    fun toDomainAudioEncoding(encoding: Int?): AudioEncoding =
        encoding?.let { AudioEncoding.entries[it] } ?: AudioEncoding.UNSPECIFIED

    // === Audio settings mapping ===

    fun toApiAudioSettings(source: AudioSettings): AudioSettingsInput =
        AudioSettingsInput(
            input = source.input?.let { toApiInput(it) },
            output = source.output?.let { toApiOutputInput(it) }
        )

    fun toDomainAudioSettings(source: AudioSettingsOutput): AudioSettings =
        AudioSettings(
            input = source.input?.let { toDomainAudioInputSettings(it) },
            output = source.output?.let { toDomainAudioOutputSettings(it) }
        )

    fun toApiInput(source: AudioInputSettings): Input =
        Input(
            model = source.model,
            audioEncoding = toApiAudioEncoding(source.audioEncoding),
            sampleRate = source.sampleRate,
            silencePhrases = source.silencePhrases.takeIf { it.isNotEmpty() },
            silencePhrasesTimeout = source.silencePhrasesTimeout?.let { TypeConverters.durationToString(it) },
            silenceTimeout = source.silenceTimeout?.let { TypeConverters.durationToString(it) },
            stopPhrases = source.stopPhrases.takeIf { it.isNotEmpty() },
            ignorePhrases = source.ignorePhrases.takeIf { it.isNotEmpty() }
        )

    fun toDomainAudioInputSettings(source: Input): AudioInputSettings =
        AudioInputSettings(
            model = source.model,
            audioEncoding = toDomainAudioEncoding(source.audioEncoding),
            sampleRate = source.sampleRate,
            silencePhrases = source.silencePhrases ?: emptyList(),
            silencePhrasesTimeout = TypeConverters.stringToDuration(source.silencePhrasesTimeout),
            silenceTimeout = TypeConverters.stringToDuration(source.silenceTimeout),
            stopPhrases = source.stopPhrases ?: emptyList(),
            ignorePhrases = source.ignorePhrases ?: emptyList()
        )

    fun toApiOutputInput(source: AudioOutputSettings): OutputInput =
        OutputInput(
            voice = source.voice,
            audioEncoding = toApiAudioEncoding(source.audioEncoding),
            stubSounds = source.stubSounds?.let { toApiStubSoundsInput(it) }
        )

    fun toDomainAudioOutputSettings(source: OutputOutput): AudioOutputSettings =
        AudioOutputSettings(
            voice = source.voice,
            audioEncoding = toDomainAudioEncoding(source.audioEncoding),
            stubSounds = source.stubSounds?.let { toDomainStubSounds(it) }
        )

    private fun toApiStubSoundsInput(source: StubSounds): StubSoundsInput =
        StubSoundsInput(
            triggerFunction = source.triggerFunction?.let { toApiTriggerFunction(it) }
                ?: ApiTriggerFunction(enable = false, mode = ApiTriggerFunctionMode._0, functionNames = emptyList()),
            sounds = source.sounds
        )

    private fun toDomainStubSounds(source: StubSoundsOutput): StubSounds =
        StubSounds(
            triggerGeneration = null,
            triggerFunction = toDomainTriggerFunction(source.triggerFunction),
            sounds = source.sounds
        )

    private fun toApiTriggerFunction(source: DomainTriggerFunction): ApiTriggerFunction =
        ApiTriggerFunction(
            enable = source.enable,
            mode = toApiTriggerFunctionMode(source.mode),
            functionNames = source.functionNames
        )

    private fun toDomainTriggerFunction(source: ApiTriggerFunction): DomainTriggerFunction =
        DomainTriggerFunction(
            enable = source.enable,
            mode = toDomainTriggerFunctionMode(source.mode),
            functionNames = source.functionNames
        )

    private fun toApiTriggerFunctionMode(mode: TriggerFunctionMode): ApiTriggerFunctionMode =
        when (mode) {
            TriggerFunctionMode.UNSPECIFIED -> ApiTriggerFunctionMode._0
            TriggerFunctionMode.WHITELIST -> ApiTriggerFunctionMode._1
            TriggerFunctionMode.BLACKLIST -> ApiTriggerFunctionMode._2
        }

    private fun toDomainTriggerFunctionMode(mode: ApiTriggerFunctionMode): TriggerFunctionMode =
        when (mode) {
            ApiTriggerFunctionMode._0 -> TriggerFunctionMode.UNSPECIFIED
            ApiTriggerFunctionMode._1 -> TriggerFunctionMode.WHITELIST
            ApiTriggerFunctionMode._2 -> TriggerFunctionMode.BLACKLIST
        }

    // === GigaChat settings mapping ===

    fun toApiGigaChatSettings(source: GigaChatSettings?): GigaChatSettingsInput? =
        source?.let {
            GigaChatSettingsInput(
                model = it.model,
                temperature = TypeConverters.floatToBigDecimal(it.temperature),
                topP = TypeConverters.floatToBigDecimal(it.topP),
                repetitionPenalty = TypeConverters.floatToBigDecimal(it.repetitionPenalty),
                updateInterval = TypeConverters.floatToBigDecimal(it.updateInterval),
                profanityCheck = it.profanityCheck,
                filtersSettings = it.filtersSettings.mapValues { entry -> toApiFilterSettings(entry.value) }
                    .takeIf { map -> map.isNotEmpty() },
                functions = it.functions.map { func -> toApiFunctionDefinition(func) },
                functionRegistry = it.functionRegistry?.let { reg -> toApiFunctionRegistry(reg) },
                filterStubPhrases = it.filterStubPhrases.takeIf { list -> list.isNotEmpty() },
                currentTime = it.currentTime,
                functionRanker = it.functionRanker?.let { fr -> toApiFunctionRanker(fr) }
            )
        }

    fun toDomainGigaChatSettings(source: GigaChatSettingsOutput?): GigaChatSettings? =
        source?.let { it ->
            GigaChatSettings(
                model = it.model,
                temperature = TypeConverters.bigDecimalToFloat(it.temperature),
                topP = TypeConverters.bigDecimalToFloat(it.topP),
                repetitionPenalty = TypeConverters.bigDecimalToFloat(it.repetitionPenalty),
                updateInterval = TypeConverters.bigDecimalToFloat(it.updateInterval),
                profanityCheck = it.profanityCheck,
                filtersSettings = it.filtersSettings?.mapValues { entry -> toDomainFilterSettings(entry.value) }
                    ?: emptyMap(),
                functions = it.functions?.map { toDomainFunctionDefinition(it) }
                    ?: emptyList(),
                functionRegistry = it.functionRegistry?.let { reg -> toDomainFunctionRegistry(reg) },
                filterStubPhrases = it.filterStubPhrases ?: emptyList(),
                currentTime = it.currentTime,
                functionRanker = it.functionRanker?.let { fr -> toDomainFunctionRanker(fr) }
            )
        }

    private fun toApiFunctionRanker(source: DomainFunctionRanker): ApiFunctionRanker =
        ApiFunctionRanker(
            enabled = source.enabled,
            topN = source.topN
        )

    private fun toDomainFunctionRanker(source: ApiFunctionRanker): DomainFunctionRanker =
        DomainFunctionRanker(
            enabled = source.enabled,
            topN = source.topN
        )

    // === Function mapping (Domain → API) ===

    private fun toApiFunctionDefinition(source: FunctionDefinition): FunctionInput =
        FunctionInput(
            name = source.name,
            description = source.description,
            parameters = source.parameters,
            fewShotExamples = source.fewShotExamples.map { toApiFunctionExample(it) },
            returnParameters = source.returnParameters
        )

    private fun toApiFunctionExample(source: FunctionExample): AnyExampleInput =
        AnyExampleInput(
            request = source.request,
            params = ru.sbrf.dab2c.executor.clients.giga.agent.model.Params(
                pairs = source.params.map { (key, value) ->
                    ru.sbrf.dab2c.executor.clients.giga.agent.model.Pair(
                        key = key,
                        value = value
                    )
                }
            )
        )

    // === Function mapping (API → Domain) ===

    fun toDomainFunctionDefinition(source: FunctionOutput): FunctionDefinition =
        FunctionDefinition(
            name = source.name,
            description = source.description,
            parameters = source.parameters,
            fewShotExamples = source.fewShotExamples?.map { toDomainFunctionExample(it) }
                ?: emptyList(),
            returnParameters = source.returnParameters
        )

    fun toDomainFunctionExample(source: AnyExampleOutput): FunctionExample =
        FunctionExample(
            request = source.request,
            params = source.params.pairs.map { it.key to it.value }
        )

    fun toApiFilterSettings(source: FilterSettings): ApiFilterSettings =
        ApiFilterSettings(
            requestContent = source.requestContent?.let {
                ru.sbrf.dab2c.executor.clients.giga.agent.model.RequestContentSettings(
                    neuro = it.neuro,
                    blacklist = it.blacklist,
                    whitelist = it.whitelist
                )
            },
            responseContent = source.responseContent?.let {
                ru.sbrf.dab2c.executor.clients.giga.agent.model.ResponseContentSettings(
                    blacklist = it.blacklist
                )
            }
        )

    fun toDomainFilterSettings(source: ApiFilterSettings): FilterSettings =
        FilterSettings(
            requestContent = source.requestContent?.let {
                ru.sbrf.dab2c.executor.domain.voice.RequestContentSettings(
                    neuro = it.neuro,
                    blacklist = it.blacklist,
                    whitelist = it.whitelist
                )
            },
            responseContent = source.responseContent?.let {
                ru.sbrf.dab2c.executor.domain.voice.ResponseContentSettings(
                    blacklist = it.blacklist
                )
            }
        )

    fun toApiFunctionRegistry(source: FunctionRegistry): ApiFunctionRegistry =
        ApiFunctionRegistry(
            profile = source.profile,
            labels = source.labels.takeIf { it.isNotEmpty() },
            abFlags = source.abFlags
        )

    fun toDomainFunctionRegistry(source: ApiFunctionRegistry): FunctionRegistry =
        FunctionRegistry(
            profile = source.profile,
            labels = source.labels ?: emptyList(),
            abFlags = source.abFlags
        )

    // === Context mapping ===

    fun toApiInitialContext(source: InitialContext?): InitialContextInput? =
        source?.let {
            InitialContextInput(
                messages = it.messages.map { msg -> toApiMessage(msg) }
            )
        }

    fun toDomainInitialContext(source: InitialContextOutput?): InitialContext? =
        source?.let {
            InitialContext(
                messages = it.messages.map { msg -> toDomainMessage(msg) }
            )
        }

    fun toApiMessage(source: Message): ApiMessage =
        ApiMessage(
            role = source.role,
            content = source.content,
            functionCall = source.functionCall?.let {
                ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCall(
                    name = it.name,
                    arguments = it.arguments
                )
            },
            functionName = source.functionName,
            functionsStateId = source.functionsStateId,
            attachments = source.attachments.takeIf { it.isNotEmpty() },
            inlineData = source.inlineData.takeIf { it.isNotEmpty() }
        )

    fun toDomainMessage(source: ApiMessage): Message =
        Message(
            role = source.role,
            content = source.content,
            functionCall = source.functionCall?.let {
                ru.sbrf.dab2c.executor.domain.voice.FunctionCall(
                    name = it.name,
                    arguments = it.arguments.orEmpty()
                )
            },
            functionName = source.functionName,
            functionsStateId = source.functionsStateId,
            attachments = source.attachments ?: emptyList(),
            inlineData = source.inlineData ?: emptyMap()
        )

    // === FirstSpeaker mapping ===

    fun toApiFirstSpeaker(source: FirstSpeaker?): ApiFirstSpeaker? =
        source?.let {
            ApiFirstSpeaker(
                type = it.type,
                lockFirstIn = it.lockFirstIn
            )
        }

    fun toDomainFirstSpeaker(source: ApiFirstSpeaker?): FirstSpeaker? =
        source?.let {
            FirstSpeaker(
                type = it.type,
                lockFirstIn = it.lockFirstIn
            )
        }

    // === Function Calling mapping ===

    fun toApiFunctionCalling(source: FunctionCallingData): ApiFunctionCalling =
        ApiFunctionCalling(
            functionCall = ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCall(
                name = source.functionCall.name,
                arguments = source.functionCall.arguments
            ),
            timestamp = source.timestamp.toInt()
        )

    fun toDomainFunctionResult(source: FunctionResult): FunctionResultData =
        FunctionResultData(
            content = source.content,
            functionName = source.functionName
        )
}
