@file:Suppress("UndocumentedPublicFunction")

package ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.mapper

import ru.sbrf.dab2c.executor.clients.giga.agent.model.AudioSettingsInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AudioSettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaChatSettingsInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaChatSettingsOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceMode
import ru.sbrf.dab2c.executor.clients.giga.agent.model.InitialContextInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.InitialContextOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Input
import ru.sbrf.dab2c.executor.clients.giga.agent.model.OutputInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.OutputOutput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Performers
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsInput
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsOutput
import ru.sbrf.dab2c.executor.domain.voice.AudioEncoding
import ru.sbrf.dab2c.executor.domain.voice.AudioInputSettings
import ru.sbrf.dab2c.executor.domain.voice.AudioOutputSettings
import ru.sbrf.dab2c.executor.domain.voice.AudioSettings
import ru.sbrf.dab2c.executor.domain.voice.FilterSettings
import ru.sbrf.dab2c.executor.domain.voice.FirstSpeaker
import ru.sbrf.dab2c.executor.domain.voice.FunctionOptions
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.domain.voice.FunctionRegistry
import ru.sbrf.dab2c.executor.domain.voice.GigaChatSettings
import ru.sbrf.dab2c.executor.domain.voice.InitialContext
import ru.sbrf.dab2c.executor.domain.voice.Message
import ru.sbrf.dab2c.executor.domain.voice.OutputModalities
import ru.sbrf.dab2c.executor.domain.voice.VoiceMode
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.clients.giga.agent.model.AudioEncoding as ApiAudioEncoding
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FilterSettings as ApiFilterSettings
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FirstSpeaker as ApiFirstSpeaker
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionOptions as ApiFunctionOptions
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionRegistry as ApiFunctionRegistry
import ru.sbrf.dab2c.executor.clients.giga.agent.model.Message as ApiMessage
import ru.sbrf.dab2c.executor.clients.giga.agent.model.OutputModalities as ApiOutputModalities

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
        outputModalities = toApiOutputModalities(source.outputModalities),
        mode = toApiVoiceMode(source.mode),
        firstSpeaker = toApiFirstSpeaker(source.firstSpeaker)
    )

    // === SettingsOutput -> VoiceSettings ===
    fun toDomainSettings(source: SettingsOutput): VoiceSettings = VoiceSettings(
        voiceCallId = source.voiceCallId,
        audio = toDomainAudioSettings(source.audio),
        gigachat = toDomainGigaChatSettings(source.gigachat),
        context = toDomainInitialContext(source.context),
        outputModalities = toDomainOutputModalities(source.outputModalities),
        mode = toDomainVoiceMode(source.mode),
        firstSpeaker = toDomainFirstSpeaker(source.firstSpeaker)
    )

    // === Performers -> FunctionPerformers ===
    fun toDomainPerformers(source: Performers): FunctionPerformers = FunctionPerformers(
        functions = source.functions.mapValues { toDomainFunctionOptions(it.value) }
    )

    fun toDomainFunctionOptions(source: ApiFunctionOptions): FunctionOptions = FunctionOptions(
        isBackendFunction = source.isBackendFunction
    )

    // === Helper methods for enum conversions ===

    fun toApiVoiceMode(mode: VoiceMode): GigaVoiceMode =
        GigaVoiceMode.entries[mode.ordinal]

    fun toDomainVoiceMode(mode: GigaVoiceMode?): VoiceMode =
        mode?.let { VoiceMode.entries[it.value] } ?: VoiceMode.UNSPECIFIED

    fun toApiOutputModalities(modalities: OutputModalities): ApiOutputModalities =
        ApiOutputModalities.entries[modalities.ordinal]

    fun toDomainOutputModalities(modalities: ApiOutputModalities?): OutputModalities =
        modalities?.let { OutputModalities.entries[it.value] } ?: OutputModalities.UNSPECIFIED

    fun toApiAudioEncoding(encoding: AudioEncoding): ApiAudioEncoding =
        ApiAudioEncoding.entries[encoding.ordinal]

    fun toDomainAudioEncoding(encoding: ApiAudioEncoding?): AudioEncoding =
        encoding?.let { AudioEncoding.entries[it.value] } ?: AudioEncoding.UNSPECIFIED

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
            audioEncoding = toApiAudioEncoding(source.audioEncoding)
        )

    fun toDomainAudioOutputSettings(source: OutputOutput): AudioOutputSettings =
        AudioOutputSettings(
            voice = source.voice,
            audioEncoding = toDomainAudioEncoding(source.audioEncoding)
        )

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
                functions = null, // Functions are handled separately via functionRegistry
                functionRegistry = it.functionRegistry?.let { reg -> toApiFunctionRegistry(reg) }
            )
        }

    fun toDomainGigaChatSettings(source: GigaChatSettingsOutput?): GigaChatSettings? =
        source?.let {
            GigaChatSettings(
                model = it.model,
                temperature = TypeConverters.bigDecimalToFloat(it.temperature),
                topP = TypeConverters.bigDecimalToFloat(it.topP),
                repetitionPenalty = TypeConverters.bigDecimalToFloat(it.repetitionPenalty),
                updateInterval = TypeConverters.bigDecimalToFloat(it.updateInterval),
                profanityCheck = it.profanityCheck,
                filtersSettings = it.filtersSettings?.mapValues { entry -> toDomainFilterSettings(entry.value) }
                    ?: emptyMap(),
                functions = emptyList(), // Functions handled separately
                functionRegistry = it.functionRegistry?.let { reg -> toDomainFunctionRegistry(reg) }
            )
        }

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
            attachments = source.attachments.takeIf { it.isNotEmpty() }
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
            attachments = source.attachments ?: emptyList()
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
}
