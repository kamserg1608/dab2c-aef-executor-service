@file:Suppress("TooManyFunctions")

package ru.sbrf.dab2c.executor.clients.gigavoice.mapper

import ru.sbrf.dab2c.executor.clients.converter.ProtoTypeConverters
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.AnyExample
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Context
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FirstSpeaker
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCall
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Input
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Message
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Output
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.anyExample
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audioSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.context
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.disableInterruption
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.filterSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.firstSpeaker
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.function
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCall
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCalling
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
import ru.sbrf.dab2c.executor.domain.voice.AudioEncoding
import ru.sbrf.dab2c.executor.domain.voice.AudioInputSettings
import ru.sbrf.dab2c.executor.domain.voice.AudioOutputSettings
import ru.sbrf.dab2c.executor.domain.voice.AudioSettings
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.DisableInterruption
import ru.sbrf.dab2c.executor.domain.voice.FilterSettings
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.FunctionDefinition
import ru.sbrf.dab2c.executor.domain.voice.FunctionExample
import ru.sbrf.dab2c.executor.domain.voice.FunctionRegistry
import ru.sbrf.dab2c.executor.domain.voice.FunctionResultData
import ru.sbrf.dab2c.executor.domain.voice.FunctionSoundRule
import ru.sbrf.dab2c.executor.domain.voice.GigaChatSettings
import ru.sbrf.dab2c.executor.domain.voice.InitialContext
import ru.sbrf.dab2c.executor.domain.voice.LockFunctionExecution
import ru.sbrf.dab2c.executor.domain.voice.OutputModalities
import ru.sbrf.dab2c.executor.domain.voice.RequestContentSettings
import ru.sbrf.dab2c.executor.domain.voice.ResponseContentSettings
import ru.sbrf.dab2c.executor.domain.voice.Speed
import ru.sbrf.dab2c.executor.domain.voice.StubSounds
import ru.sbrf.dab2c.executor.domain.voice.TriggerFunctionMode
import ru.sbrf.dab2c.executor.domain.voice.VoiceMode
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.AudioSettings as ProtoAudioSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.DisableInterruption as ProtoDisableInterruption
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FilterSettings as ProtoFilterSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Function as ProtoFunction
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionRanker as ProtoFunctionRanker
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionRegistry as ProtoFunctionRegistry
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionSoundRule as ProtoFunctionSoundRule
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaChatSettings as ProtoGigaChatSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.InitialContext as ProtoInitialContext
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.LockFunctionExecution as ProtoLockFunctionExecution
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.RequestContentSettings as ProtoRequestContentSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.ResponseContentSettings as ProtoResponseContentSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.StubSounds as ProtoStubSounds
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.TriggerFunction as ProtoTriggerFunction
import ru.sbrf.dab2c.executor.domain.voice.FirstSpeaker as DomainFirstSpeaker
import ru.sbrf.dab2c.executor.domain.voice.FunctionCall as DomainFunctionCall
import ru.sbrf.dab2c.executor.domain.voice.FunctionRanker as DomainFunctionRanker
import ru.sbrf.dab2c.executor.domain.voice.Message as DomainMessage

// Top-level bridges used by the chunk-dispatch service layer.

/** Converts proto Settings to domain VoiceSettings. */
fun Settings.toDomain(): VoiceSettings = settingsToDomain(this)

/** Converts domain VoiceSettings to proto Settings. */
fun VoiceSettings.toProto(): Settings = settingsToProto(this)

/** Converts proto Context to domain ContextData. */
fun Context.toDomain(): ContextData = ContextData(content = content)

/** Converts domain ContextData to proto Context. */
fun ContextData.toProto(): Context = context { content = this@toProto.content }

/** Converts proto FunctionCalling to domain FunctionCallingData. */
fun FunctionCalling.toDomain(): FunctionCallingData = FunctionCallingData(
    functionCall = functionCall.toDomain(),
    timestamp = timestamp
)

/** Converts domain FunctionCallingData to proto FunctionCalling. */
fun FunctionCallingData.toProto(): FunctionCalling = functionCalling {
    functionCall = this@toProto.functionCall.toProto()
    timestamp = this@toProto.timestamp
}

/** Converts proto FunctionCall to domain FunctionCall. */
fun FunctionCall.toDomain(): DomainFunctionCall = DomainFunctionCall(
    name = name,
    arguments = arguments
)

/** Converts domain FunctionCall to proto FunctionCall. */
fun DomainFunctionCall.toProto(): FunctionCall = functionCall {
    name = this@toProto.name
    arguments = this@toProto.arguments
}

/** Converts domain FunctionResultData to proto FunctionResult. */
fun FunctionResultData.toProto(): FunctionResult = functionResult {
    content = this@toProto.content
    this@toProto.functionName?.let { functionName = it }
}

// ==================== Settings proto -> domain ====================

private fun settingsToDomain(s: Settings): VoiceSettings = VoiceSettings(
    voiceCallId = s.voiceCallId,
    audio = toDomainAudioSettings(s.audio),
    gigachat = if (s.hasGigachat()) toDomainGigaChatSettings(s.gigachat) else null,
    context = if (s.hasContext()) toDomainInitialContext(s.context) else null,
    disableVad = s.disableVad,
    enableTranscribeInput = s.enableTranscribeInput,
    flags = s.flagsList,
    outputModalities = toDomainOutputModalities(s.outputModalities),
    mode = toDomainVoiceMode(s.mode),
    firstSpeaker = if (s.hasFirstSpeaker()) toDomainFirstSpeaker(s.firstSpeaker) else null,
    enableDenoiser = s.enableDenoiser,
    enablePrefetch = s.enablePrefetch,
    enablePersonIdentity = s.enablePersonIdentity,
    enableWhisper = s.enableWhisper,
    enableEmotion = s.enableEmotion,
    enableTranscribeSilencePhrases = s.enableTranscribeSilencePhrases,
    disableInterruption = if (s.hasDisableInterruption()) toDomainDisableInterruption(s.disableInterruption) else null,
)

private fun toDomainAudioSettings(a: ProtoAudioSettings): AudioSettings = AudioSettings(
    input = if (a.hasInput()) toDomainInputSettings(a.input) else null,
    output = if (a.hasOutput()) toDomainOutputSettings(a.output) else null
)

private fun toDomainInputSettings(i: Input): AudioInputSettings = AudioInputSettings(
    model = i.model.takeIf { it.isNotEmpty() },
    audioEncoding = toDomainInputAudioEncoding(i.audioEncoding),
    sampleRate = if (i.hasSampleRate()) i.sampleRate else null,
    silencePhrases = i.silencePhrasesList,
    silencePhrasesTimeout = if (i.hasSilencePhrasesTimeout()) {
        ProtoTypeConverters.protoDurationToKotlinDuration(i.silencePhrasesTimeout)
    } else {
        null
    },
    silenceTimeout = if (i.hasSilenceTimeout()) {
        ProtoTypeConverters.protoDurationToKotlinDuration(i.silenceTimeout)
    } else {
        null
    },
    stopPhrases = i.stopPhrasesList,
    ignorePhrases = i.ignorePhrasesList
)

private fun toDomainOutputSettings(o: Output): AudioOutputSettings = AudioOutputSettings(
    voice = o.voice.takeIf { it.isNotEmpty() },
    audioEncoding = toDomainOutputAudioEncoding(o.audioEncoding),
    stubSounds = if (o.hasStubSounds()) toDomainStubSounds(o.stubSounds) else null,
    speed = toDomainOutputSpeed(o.speed),
)

private fun toDomainStubSounds(p: ProtoStubSounds): StubSounds = StubSounds(
    triggerGeneration = if (p.hasTriggerGeneration()) {
        ru.sbrf.dab2c.executor.domain.voice.TriggerGeneration(
            timeout = if (p.triggerGeneration.hasTimeout()) {
                ProtoTypeConverters.protoDurationToKotlinDuration(p.triggerGeneration.timeout)
            } else {
                null
            },
            enable = p.triggerGeneration.enable
        )
    } else {
        null
    },
    triggerFunction = if (p.hasTriggerFunction()) {
        ru.sbrf.dab2c.executor.domain.voice.TriggerFunction(
            enable = p.triggerFunction.enable,
            mode = toDomainTriggerFunctionMode(p.triggerFunction.mode),
            functionNames = p.triggerFunction.functionNamesList,
            rules = p.triggerFunction.rulesList.map { toDomainTriggerFunctionRule(it) }
        )
    } else {
        null
    },
    sounds = p.soundsList
)

private fun toDomainTriggerFunctionRule(r: ProtoFunctionSoundRule): FunctionSoundRule = FunctionSoundRule(
    functionNames = r.functionNamesList,
    sounds = r.soundsList,
)

private fun toDomainTriggerFunctionMode(m: ProtoTriggerFunction.Mode): TriggerFunctionMode = when (m) {
    ProtoTriggerFunction.Mode.MODE_UNSPECIFIED,
    ProtoTriggerFunction.Mode.UNRECOGNIZED -> TriggerFunctionMode.UNSPECIFIED
    ProtoTriggerFunction.Mode.WHITELIST -> TriggerFunctionMode.WHITELIST
    ProtoTriggerFunction.Mode.BLACKLIST -> TriggerFunctionMode.BLACKLIST
}

private fun toDomainInputAudioEncoding(e: Input.AudioEncoding): AudioEncoding = when (e) {
    Input.AudioEncoding.AUDIO_ENCODING_UNSPECIFIED, Input.AudioEncoding.UNRECOGNIZED -> AudioEncoding.UNSPECIFIED
    Input.AudioEncoding.PCM_S16LE -> AudioEncoding.PCM_S16LE
    Input.AudioEncoding.OPUS -> AudioEncoding.OPUS
    Input.AudioEncoding.PCM_ALAW -> AudioEncoding.PCM_ALAW
}

private fun toDomainOutputAudioEncoding(e: Output.AudioEncoding): AudioEncoding = when (e) {
    Output.AudioEncoding.AUDIO_ENCODING_UNSPECIFIED, Output.AudioEncoding.UNRECOGNIZED -> AudioEncoding.UNSPECIFIED
    Output.AudioEncoding.PCM_S16LE -> AudioEncoding.PCM_S16LE
    Output.AudioEncoding.OPUS -> AudioEncoding.OPUS
    Output.AudioEncoding.PCM_ALAW -> AudioEncoding.PCM_ALAW
}

private fun toDomainOutputSpeed(s: Output.Speed): Speed = when (s) {
    Output.Speed.SPEED_UNSPECIFIED, Output.Speed.UNRECOGNIZED -> Speed.SPEED_UNSPECIFIED
    Output.Speed.EXTRA_SLOW -> Speed.EXTRA_SLOW
    Output.Speed.SLOW -> Speed.SLOW
    Output.Speed.MEDIUM -> Speed.MEDIUM
    Output.Speed.FAST -> Speed.FAST
    Output.Speed.EXTRA_FAST -> Speed.EXTRA_FAST
}

private fun toDomainGigaChatSettings(g: ProtoGigaChatSettings): GigaChatSettings = GigaChatSettings(
    model = g.model.takeIf { it.isNotEmpty() },
    temperature = if (g.hasTemperature()) g.temperature else null,
    topP = if (g.hasTopP()) g.topP else null,
    repetitionPenalty = if (g.hasRepetitionPenalty()) g.repetitionPenalty else null,
    updateInterval = if (g.hasUpdateInterval()) g.updateInterval else null,
    profanityCheck = if (g.hasProfanityCheck()) g.profanityCheck else null,
    filtersSettings = g.filtersSettingsMap.mapValues { toDomainFilterSettings(it.value) },
    functions = g.functionsList.map { toDomainFunction(it) },
    functionRegistry = if (g.hasFunctionRegistry()) toDomainFunctionRegistry(g.functionRegistry) else null,
    filterStubPhrases = g.filterStubPhrasesList,
    currentTime = if (g.hasCurrentTime()) g.currentTime else null,
    functionRanker = if (g.hasFunctionRanker()) toDomainFunctionRanker(g.functionRanker) else null,
    preset = if (g.hasPreset()) g.preset else null,
)

private fun toDomainFunctionRanker(p: ProtoFunctionRanker): DomainFunctionRanker = DomainFunctionRanker(
    enabled = if (p.hasEnabled()) p.enabled else null,
    topN = if (p.hasTopN()) p.topN.toInt() else null,
    embedderModel = if (p.hasEmbedderModel()) p.embedderModel else null,
    ignoredFunctions = p.ignoredFunctionsList,
)

private fun toDomainFilterSettings(f: ProtoFilterSettings): FilterSettings = FilterSettings(
    requestContent = if (f.hasRequestContent()) toDomainRequestContentSettings(f.requestContent) else null,
    responseContent = if (f.hasResponseContent()) toDomainResponseContentSettings(f.responseContent) else null
)

private fun toDomainRequestContentSettings(s: ProtoRequestContentSettings): RequestContentSettings =
    RequestContentSettings(
        neuro = if (s.hasNeuro()) s.neuro else null,
        blacklist = if (s.hasBlacklist()) s.blacklist else null,
        whitelist = if (s.hasWhitelist()) s.whitelist else null
    )

private fun toDomainResponseContentSettings(s: ProtoResponseContentSettings): ResponseContentSettings =
    ResponseContentSettings(blacklist = if (s.hasBlacklist()) s.blacklist else null)

private fun toDomainFunction(f: ProtoFunction): FunctionDefinition = FunctionDefinition(
    name = f.name,
    description = f.description.takeIf { it.isNotEmpty() },
    parameters = f.parameters.takeIf { it.isNotEmpty() },
    fewShotExamples = f.fewShotExamplesList.map { toDomainFunctionExample(it) },
    returnParameters = f.returnParameters.takeIf { it.isNotEmpty() }
)

private fun toDomainFunctionExample(e: AnyExample): FunctionExample = FunctionExample(
    request = e.request,
    params = e.params.pairsList.map { it.key to it.value }
)

private fun toDomainFunctionRegistry(r: ProtoFunctionRegistry): FunctionRegistry = FunctionRegistry(
    profile = r.profile.takeIf { it.isNotEmpty() },
    labels = r.labelsList,
    abFlags = r.abFlags.takeIf { it.isNotEmpty() }
)

private fun toDomainInitialContext(c: ProtoInitialContext): InitialContext = InitialContext(
    messages = c.messagesList.map { toDomainMessage(it) }
)

private fun toDomainMessage(m: Message): DomainMessage = DomainMessage(
    role = m.role,
    content = m.content,
    functionCall = if (m.hasFunctionCall()) m.functionCall.toDomain() else null,
    functionName = m.functionName.takeIf { it.isNotEmpty() },
    functionsStateId = m.functionsStateId.takeIf { it.isNotEmpty() },
    attachments = m.attachmentsList,
    inlineData = m.inlineDataMap,
    functions = m.functionsList.map { toDomainFunction(it) }
)

private fun toDomainFirstSpeaker(f: FirstSpeaker): DomainFirstSpeaker = DomainFirstSpeaker(
    type = f.type.takeIf { it.isNotEmpty() },
    lockFirstIn = if (f.hasLockFirstIn()) f.lockFirstIn else null
)

private fun toDomainVoiceMode(m: Settings.Mode): VoiceMode = when (m) {
    Settings.Mode.MODE_UNSPECIFIED, Settings.Mode.UNRECOGNIZED -> VoiceMode.UNSPECIFIED
    Settings.Mode.RECOGNIZE_GIGACHAT_SYNTHESIS -> VoiceMode.RECOGNIZE_GIGACHAT_SYNTHESIS
    Settings.Mode.GIGACHAT_SYNTHESIS -> VoiceMode.GIGACHAT_SYNTHESIS
    Settings.Mode.GIGACHAT -> VoiceMode.GIGACHAT
    Settings.Mode.RECOGNIZE_SYNTHESIS -> VoiceMode.RECOGNIZE_SYNTHESIS
}

private fun toDomainOutputModalities(m: Settings.OutputModalities): OutputModalities = when (m) {
    Settings.OutputModalities.MODALITIES_UNSPECIFIED,
    Settings.OutputModalities.UNRECOGNIZED -> OutputModalities.UNSPECIFIED
    Settings.OutputModalities.AUDIO -> OutputModalities.AUDIO
    Settings.OutputModalities.AUDIO_TEXT -> OutputModalities.AUDIO_TEXT
    Settings.OutputModalities.TEXT -> OutputModalities.TEXT
}

private fun toDomainDisableInterruption(d: ProtoDisableInterruption): DisableInterruption = DisableInterruption(
    functions = d.functionsList.map { toDomainLockFunctionExecution(it) }
)

private fun toDomainLockFunctionExecution(l: ProtoLockFunctionExecution): LockFunctionExecution = LockFunctionExecution(
    name = l.name,
    onExecution = l.onExecution,
    afterResult = l.afterResult
)

// ==================== Settings domain -> proto ====================

private fun settingsToProto(d: VoiceSettings): Settings = settings {
    voiceCallId = d.voiceCallId
    audio = toProtoAudioSettings(d.audio)
    d.gigachat?.let { gigachat = toProtoGigaChatSettings(it) }
    d.context?.let { context = toProtoInitialContext(it) }
    disableVad = d.disableVad
    enableTranscribeInput = d.enableTranscribeInput
    flags.addAll(d.flags)
    outputModalities = toProtoOutputModalities(d.outputModalities)
    mode = toProtoVoiceMode(d.mode)
    d.firstSpeaker?.let { firstSpeaker = toProtoFirstSpeaker(it) }
    enableDenoiser = d.enableDenoiser
    enablePrefetch = d.enablePrefetch
    enablePersonIdentity = d.enablePersonIdentity
    enableWhisper = d.enableWhisper
    enableEmotion = d.enableEmotion
    enableTranscribeSilencePhrases = d.enableTranscribeSilencePhrases
    d.disableInterruption?.let { disableInterruption = toProtoDisableInterruption(it) }
}

private fun toProtoDisableInterruption(i: DisableInterruption): ProtoDisableInterruption = disableInterruption {
    functions.addAll(i.functions.map { toProtoLockFunctionExecution(it) })
}

private fun toProtoLockFunctionExecution(l: LockFunctionExecution): ProtoLockFunctionExecution = lockFunctionExecution {
    name = l.name
    onExecution = l.onExecution
    afterResult = l.afterResult
}

private fun toProtoAudioSettings(a: AudioSettings): ProtoAudioSettings = audioSettings {
    a.input?.let { input = toProtoInput(it) }
    a.output?.let { output = toProtoOutput(it) }
}

private fun toProtoInput(i: AudioInputSettings): Input = input {
    i.model?.let { model = it }
    audioEncoding = toProtoInputAudioEncoding(i.audioEncoding)
    i.sampleRate?.let { sampleRate = it }
    silencePhrases.addAll(i.silencePhrases)
    i.silencePhrasesTimeout?.let { silencePhrasesTimeout = ProtoTypeConverters.kotlinDurationToProtoDuration(it) }
    i.silenceTimeout?.let { silenceTimeout = ProtoTypeConverters.kotlinDurationToProtoDuration(it) }
    stopPhrases.addAll(i.stopPhrases)
    ignorePhrases.addAll(i.ignorePhrases)
}

private fun toProtoOutput(o: AudioOutputSettings): Output = output {
    o.voice?.let { voice = it }
    audioEncoding = toProtoOutputAudioEncoding(o.audioEncoding)
    o.stubSounds?.let { stubSounds = toProtoStubSounds(it) }
    speed = toProtoSpeed(o.speed)
}

private fun toProtoStubSounds(s: StubSounds): ProtoStubSounds = stubSounds {
    s.triggerGeneration?.let { tg ->
        triggerGeneration = triggerGeneration {
            tg.timeout?.let { timeout = ProtoTypeConverters.kotlinDurationToProtoDuration(it) }
            enable = tg.enable
        }
    }
    s.triggerFunction?.let { tf ->
        triggerFunction = triggerFunction {
            enable = tf.enable
            mode = toProtoTriggerFunctionMode(tf.mode)
            functionNames.addAll(tf.functionNames)
            rules.addAll(tf.rules.map { toProtoFunctionSoundRule(it) })
        }
    }
    sounds.addAll(s.sounds)
}

private fun toProtoFunctionSoundRule(r: FunctionSoundRule): ProtoFunctionSoundRule = functionSoundRule {
    functionNames.addAll(r.functionNames)
    sounds.addAll(r.sounds)
}

private fun toProtoTriggerFunctionMode(m: TriggerFunctionMode): ProtoTriggerFunction.Mode = when (m) {
    TriggerFunctionMode.UNSPECIFIED -> ProtoTriggerFunction.Mode.MODE_UNSPECIFIED
    TriggerFunctionMode.WHITELIST -> ProtoTriggerFunction.Mode.WHITELIST
    TriggerFunctionMode.BLACKLIST -> ProtoTriggerFunction.Mode.BLACKLIST
}

private fun toProtoInputAudioEncoding(e: AudioEncoding): Input.AudioEncoding = when (e) {
    AudioEncoding.UNSPECIFIED -> Input.AudioEncoding.AUDIO_ENCODING_UNSPECIFIED
    AudioEncoding.PCM_S16LE -> Input.AudioEncoding.PCM_S16LE
    AudioEncoding.OPUS -> Input.AudioEncoding.OPUS
    AudioEncoding.PCM_ALAW -> Input.AudioEncoding.PCM_ALAW
}

private fun toProtoOutputAudioEncoding(e: AudioEncoding): Output.AudioEncoding = when (e) {
    AudioEncoding.UNSPECIFIED -> Output.AudioEncoding.AUDIO_ENCODING_UNSPECIFIED
    AudioEncoding.PCM_S16LE -> Output.AudioEncoding.PCM_S16LE
    AudioEncoding.OPUS -> Output.AudioEncoding.OPUS
    AudioEncoding.PCM_ALAW -> Output.AudioEncoding.PCM_ALAW
}

private fun toProtoSpeed(s: Speed): Output.Speed = when (s) {
    Speed.SPEED_UNSPECIFIED -> Output.Speed.SPEED_UNSPECIFIED
    Speed.EXTRA_SLOW -> Output.Speed.EXTRA_SLOW
    Speed.SLOW -> Output.Speed.SLOW
    Speed.MEDIUM -> Output.Speed.MEDIUM
    Speed.FAST -> Output.Speed.FAST
    Speed.EXTRA_FAST -> Output.Speed.EXTRA_FAST
}

private fun toProtoGigaChatSettings(g: GigaChatSettings): ProtoGigaChatSettings = gigaChatSettings {
    g.model?.let { model = it }
    g.temperature?.let { temperature = it }
    g.topP?.let { topP = it }
    g.repetitionPenalty?.let { repetitionPenalty = it }
    g.updateInterval?.let { updateInterval = it }
    g.profanityCheck?.let { profanityCheck = it }
    filtersSettings.putAll(g.filtersSettings.mapValues { toProtoFilterSettings(it.value) })
    functions.addAll(g.functions.map { toProtoFunction(it) })
    g.functionRegistry?.let { functionRegistry = toProtoFunctionRegistry(it) }
    filterStubPhrases.addAll(g.filterStubPhrases)
    g.currentTime?.let { currentTime = it }
    g.functionRanker?.let { functionRanker = toProtoFunctionRanker(it) }
    g.preset?.let { preset = it }
}

private fun toProtoFunctionRanker(d: DomainFunctionRanker): ProtoFunctionRanker = functionRanker {
    d.enabled?.let { enabled = it }
    d.topN?.let { topN = it }
    d.embedderModel?.let { embedderModel = it }
    ignoredFunctions.addAll(d.ignoredFunctions)
}

private fun toProtoFilterSettings(f: FilterSettings): ProtoFilterSettings = filterSettings {
    f.requestContent?.let { requestContent = toProtoRequestContentSettings(it) }
    f.responseContent?.let { responseContent = toProtoResponseContentSettings(it) }
}

private fun toProtoRequestContentSettings(
    s: RequestContentSettings
): ProtoRequestContentSettings = requestContentSettings {
    s.neuro?.let { neuro = it }
    s.blacklist?.let { blacklist = it }
    s.whitelist?.let { whitelist = it }
}

private fun toProtoResponseContentSettings(
    s: ResponseContentSettings
): ProtoResponseContentSettings = responseContentSettings {
    s.blacklist?.let { blacklist = it }
}

private fun toProtoFunction(d: FunctionDefinition): ProtoFunction = function {
    name = d.name
    d.description?.let { description = it }
    d.parameters?.let { parameters = it }
    fewShotExamples.addAll(d.fewShotExamples.map { toProtoAnyExample(it) })
    d.returnParameters?.let { returnParameters = it }
}

private fun toProtoAnyExample(e: FunctionExample): AnyExample = anyExample {
    request = e.request
    params = params {
        pairs.addAll(
            e.params.map { (k, v) ->
                pair {
                    key = k
                    value = v
                }
            }
        )
    }
}

private fun toProtoFunctionRegistry(r: FunctionRegistry): ProtoFunctionRegistry = functionRegistry {
    r.profile?.let { profile = it }
    labels.addAll(r.labels)
    r.abFlags?.let { abFlags = it }
}

private fun toProtoInitialContext(c: InitialContext): ProtoInitialContext = initialContext {
    messages.addAll(c.messages.map { toProtoMessage(it) })
}

private fun toProtoMessage(m: DomainMessage): Message = message {
    role = m.role
    content = m.content
    m.functionCall?.let { functionCall = it.toProto() }
    m.functionName?.let { functionName = it }
    m.functionsStateId?.let { functionsStateId = it }
    attachments.addAll(m.attachments)
    inlineData.putAll(m.inlineData)
    functions.addAll(m.functions.map { toProtoFunction(it) })
}

private fun toProtoFirstSpeaker(d: DomainFirstSpeaker): FirstSpeaker = firstSpeaker {
    d.type?.let { type = it }
    d.lockFirstIn?.let { lockFirstIn = it }
}

private fun toProtoVoiceMode(m: VoiceMode): Settings.Mode = when (m) {
    VoiceMode.UNSPECIFIED -> Settings.Mode.MODE_UNSPECIFIED
    VoiceMode.RECOGNIZE_GIGACHAT_SYNTHESIS -> Settings.Mode.RECOGNIZE_GIGACHAT_SYNTHESIS
    VoiceMode.GIGACHAT_SYNTHESIS -> Settings.Mode.GIGACHAT_SYNTHESIS
    VoiceMode.GIGACHAT -> Settings.Mode.GIGACHAT
    VoiceMode.RECOGNIZE_SYNTHESIS -> Settings.Mode.RECOGNIZE_SYNTHESIS
}

private fun toProtoOutputModalities(m: OutputModalities): Settings.OutputModalities = when (m) {
    OutputModalities.UNSPECIFIED -> Settings.OutputModalities.MODALITIES_UNSPECIFIED
    OutputModalities.AUDIO -> Settings.OutputModalities.AUDIO
    OutputModalities.AUDIO_TEXT -> Settings.OutputModalities.AUDIO_TEXT
    OutputModalities.TEXT -> Settings.OutputModalities.TEXT
}
