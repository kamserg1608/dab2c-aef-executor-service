package ru.sbrf.dab2c.executor.clients.gigavoice.mapper

import ru.sbrf.dab2c.executor.clients.converter.ProtoTypeConverters
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.AdditionalData
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.AgeType
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.AnyExample
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Audio
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.AudioContent
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.ContentForSynthesis
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.ContentFromClient
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.ContentFromModel
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Emotion
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Error
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FirstSpeaker
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCall
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GenderType
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaChatModelInfo
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Input
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.InputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Message
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Output
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.OutputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.PersonIdentity
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Usage
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Warning
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.additionalData
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audio
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.contentFromModel
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.emotion
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.error
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.file
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCall
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaChatModelInfo
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.inputFiles
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.inputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.outputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.personIdentity
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.platformFunctionProcessing
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.serviceInfo
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.serviceVersion
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.usage
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.warning
import ru.sbrf.dab2c.executor.domain.voice.AdditionalDataContent
import ru.sbrf.dab2c.executor.domain.voice.AudioChunkMeta
import ru.sbrf.dab2c.executor.domain.voice.AudioEncoding
import ru.sbrf.dab2c.executor.domain.voice.AudioInputSettings
import ru.sbrf.dab2c.executor.domain.voice.AudioOutput
import ru.sbrf.dab2c.executor.domain.voice.AudioOutputSettings
import ru.sbrf.dab2c.executor.domain.voice.AudioSettings
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.DisableInterruption
import ru.sbrf.dab2c.executor.domain.voice.ErrorData
import ru.sbrf.dab2c.executor.domain.voice.FilterSettings
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.FunctionDefinition
import ru.sbrf.dab2c.executor.domain.voice.FunctionExample
import ru.sbrf.dab2c.executor.domain.voice.FunctionRegistry
import ru.sbrf.dab2c.executor.domain.voice.FunctionResultData
import ru.sbrf.dab2c.executor.domain.voice.FunctionSoundRule
import ru.sbrf.dab2c.executor.domain.voice.GigaChatSettings
import ru.sbrf.dab2c.executor.domain.voice.InitialContext
import ru.sbrf.dab2c.executor.domain.voice.InputFilesData
import ru.sbrf.dab2c.executor.domain.voice.InputTranscriptionData
import ru.sbrf.dab2c.executor.domain.voice.LockFunctionExecution
import ru.sbrf.dab2c.executor.domain.voice.OutputModalities
import ru.sbrf.dab2c.executor.domain.voice.OutputTranscriptionData
import ru.sbrf.dab2c.executor.domain.voice.PlatformFunctionProcessingData
import ru.sbrf.dab2c.executor.domain.voice.RequestContentSettings
import ru.sbrf.dab2c.executor.domain.voice.ResponseContentSettings
import ru.sbrf.dab2c.executor.domain.voice.ServiceInfoData
import ru.sbrf.dab2c.executor.domain.voice.Speed
import ru.sbrf.dab2c.executor.domain.voice.StubSounds
import ru.sbrf.dab2c.executor.domain.voice.SynthesisContent
import ru.sbrf.dab2c.executor.domain.voice.SynthesisContentType
import ru.sbrf.dab2c.executor.domain.voice.TriggerFunctionMode
import ru.sbrf.dab2c.executor.domain.voice.UsageData
import ru.sbrf.dab2c.executor.domain.voice.VoiceMode
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.domain.voice.WarningData
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
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.PlatformFunctionProcessing as ProtoPlatformFunctionProcessing
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.RequestContentSettings as ProtoRequestContentSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.ResponseContentSettings as ProtoResponseContentSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.ServiceInfo as ProtoServiceInfo
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.StubSounds as ProtoStubSounds
import ru.sbrf.dab2c.executor.domain.voice.AudioContent as DomainAudioContent
import ru.sbrf.dab2c.executor.domain.voice.ContentFromModel as DomainContentFromModel
import ru.sbrf.dab2c.executor.domain.voice.Emotion as DomainEmotion
import ru.sbrf.dab2c.executor.domain.voice.FirstSpeaker as DomainFirstSpeaker
import ru.sbrf.dab2c.executor.domain.voice.FunctionCall as DomainFunctionCall
import ru.sbrf.dab2c.executor.domain.voice.FunctionRanker as DomainFunctionRanker
import ru.sbrf.dab2c.executor.domain.voice.GigaChatModelInfo as DomainGigaChatModelInfo
import ru.sbrf.dab2c.executor.domain.voice.Message as DomainMessage
import ru.sbrf.dab2c.executor.domain.voice.PersonIdentity as DomainPersonIdentity

/**
 * Mapper for IVR proto <-> domain type conversions.
 */
object IvrDomainMapper {

    // ==================== Request Mapping: Proto -> Domain (Server-side) ====================

    /** Converts IVR proto request to domain VoiceRequest. */
    fun toDomainRequest(request: GigaVoiceRequest): VoiceRequest =
        when (request.requestCase) {
            GigaVoiceRequest.RequestCase.SETTINGS ->
                VoiceRequest.Settings(toDomainSettings(request.settings))
            GigaVoiceRequest.RequestCase.INPUT ->
                toDomainContentFromClient(request.input)
            GigaVoiceRequest.RequestCase.FUNCTION_RESULT ->
                VoiceRequest.FunctionResult(toDomainFunctionResult(request.functionResult))
            GigaVoiceRequest.RequestCase.CONTEXT ->
                VoiceRequest.Context(toDomainContext(request.context))
            GigaVoiceRequest.RequestCase.REQUEST_NOT_SET, null ->
                error("Request not set")
        }

    private fun toDomainSettings(settings: Settings): VoiceSettings = VoiceSettings(
        voiceCallId = settings.voiceCallId,
        audio = toDomainAudioSettings(settings.audio),
        gigachat = if (settings.hasGigachat()) toDomainGigaChatSettings(settings.gigachat) else null,
        context = if (settings.hasContext()) toDomainInitialContext(settings.context) else null,
        disableVad = settings.disableVad,
        enableTranscribeInput = settings.enableTranscribeInput,
        flags = settings.flagsList,
        outputModalities = toDomainOutputModalities(settings.outputModalities),
        mode = toDomainVoiceMode(settings.mode),
        firstSpeaker = if (settings.hasFirstSpeaker()) {
            toDomainFirstSpeaker(settings.firstSpeaker)
        } else {
            null
        },
        enableDenoiser = settings.enableDenoiser,
        enablePrefetch = settings.enablePrefetch,
        enablePersonIdentity = settings.enablePersonIdentity,
        enableWhisper = settings.enableWhisper,
        enableEmotion = settings.enableEmotion,
        enableTranscribeSilencePhrases = settings.enableTranscribeSilencePhrases,
        disableInterruption = if (settings.hasDisableInterruption()) {
            toDomainDisableInterruption(settings.disableInterruption)
        } else {
            null
        },
    )

    private fun toDomainAudioSettings(audio: ProtoAudioSettings): AudioSettings = AudioSettings(
        input = if (audio.hasInput()) toDomainInputSettings(audio.input) else null,
        output = if (audio.hasOutput()) toDomainOutputSettings(audio.output) else null
    )

    private fun toDomainInputSettings(input: Input): AudioInputSettings = AudioInputSettings(
        model = input.model.takeIf { it.isNotEmpty() },
        audioEncoding = toDomainInputAudioEncoding(input.audioEncoding),
        sampleRate = if (input.hasSampleRate()) input.sampleRate else null,
        silencePhrases = input.silencePhrasesList,
        silencePhrasesTimeout = if (input.hasSilencePhrasesTimeout()) {
            ProtoTypeConverters.protoDurationToKotlinDuration(input.silencePhrasesTimeout)
        } else {
            null
        },
        silenceTimeout = if (input.hasSilenceTimeout()) {
            ProtoTypeConverters.protoDurationToKotlinDuration(input.silenceTimeout)
        } else {
            null
        },
        stopPhrases = input.stopPhrasesList,
        ignorePhrases = input.ignorePhrasesList
    )

    private fun toDomainOutputSettings(output: Output): AudioOutputSettings = AudioOutputSettings(
        voice = output.voice.takeIf { it.isNotEmpty() },
        audioEncoding = toDomainOutputAudioEncoding(output.audioEncoding),
        stubSounds = if (output.hasStubSounds()) toDomainStubSounds(output.stubSounds) else null,
        speed = toDomainOutputSpeed(output.speed),
    )

    private fun toDomainStubSounds(proto: ProtoStubSounds): StubSounds = StubSounds(
        triggerGeneration = if (proto.hasTriggerGeneration()) {
            ru.sbrf.dab2c.executor.domain.voice.TriggerGeneration(
                timeout = if (proto.triggerGeneration.hasTimeout()) {
                    ProtoTypeConverters.protoDurationToKotlinDuration(proto.triggerGeneration.timeout)
                } else {
                    null
                },
                enable = proto.triggerGeneration.enable
            )
        } else {
            null
        },
        triggerFunction = if (proto.hasTriggerFunction()) {
            ru.sbrf.dab2c.executor.domain.voice.TriggerFunction(
                enable = proto.triggerFunction.enable,
                mode = toDomainTriggerFunctionMode(proto.triggerFunction.mode),
                functionNames = proto.triggerFunction.functionNamesList,
                rules = proto.triggerFunction.rulesList.map { toDomainTriggerFunctionRule(it) }
            )
        } else {
            null
        },
        sounds = proto.soundsList
    )

    private fun toDomainTriggerFunctionRule(rule: ProtoFunctionSoundRule): FunctionSoundRule = FunctionSoundRule(
        functionNames = rule.functionNamesList,
        sounds = rule.soundsList,
    )

    private fun toDomainTriggerFunctionMode(
        mode: ru.sbrf.dab2c.executor.clients.gigavoice.proto.TriggerFunction.Mode
    ): TriggerFunctionMode = when (mode) {
        ru.sbrf.dab2c.executor.clients.gigavoice.proto.TriggerFunction.Mode.MODE_UNSPECIFIED,
        ru.sbrf.dab2c.executor.clients.gigavoice.proto.TriggerFunction.Mode.UNRECOGNIZED ->
            TriggerFunctionMode.UNSPECIFIED
        ru.sbrf.dab2c.executor.clients.gigavoice.proto.TriggerFunction.Mode.WHITELIST ->
            TriggerFunctionMode.WHITELIST
        ru.sbrf.dab2c.executor.clients.gigavoice.proto.TriggerFunction.Mode.BLACKLIST ->
            TriggerFunctionMode.BLACKLIST
    }

    private fun toDomainInputAudioEncoding(encoding: Input.AudioEncoding): AudioEncoding =
        when (encoding) {
            Input.AudioEncoding.AUDIO_ENCODING_UNSPECIFIED, Input.AudioEncoding.UNRECOGNIZED ->
                AudioEncoding.UNSPECIFIED
            Input.AudioEncoding.PCM_S16LE -> AudioEncoding.PCM_S16LE
            Input.AudioEncoding.OPUS -> AudioEncoding.OPUS
            Input.AudioEncoding.PCM_ALAW -> AudioEncoding.PCM_ALAW
        }

    private fun toDomainOutputAudioEncoding(encoding: Output.AudioEncoding): AudioEncoding =
        when (encoding) {
            Output.AudioEncoding.AUDIO_ENCODING_UNSPECIFIED, Output.AudioEncoding.UNRECOGNIZED ->
                AudioEncoding.UNSPECIFIED
            Output.AudioEncoding.PCM_S16LE -> AudioEncoding.PCM_S16LE
            Output.AudioEncoding.OPUS -> AudioEncoding.OPUS
            Output.AudioEncoding.PCM_ALAW -> AudioEncoding.PCM_ALAW
        }

    private fun toDomainOutputSpeed(speed: Output.Speed): Speed =
        when (speed) {
            Output.Speed.SPEED_UNSPECIFIED, Output.Speed.UNRECOGNIZED -> Speed.SPEED_UNSPECIFIED
            Output.Speed.EXTRA_SLOW -> Speed.EXTRA_SLOW
            Output.Speed.SLOW -> Speed.SLOW
            Output.Speed.MEDIUM -> Speed.MEDIUM
            Output.Speed.FAST -> Speed.FAST
            Output.Speed.EXTRA_FAST -> Speed.EXTRA_FAST
        }

    private fun toDomainGigaChatSettings(settings: ProtoGigaChatSettings): GigaChatSettings =
        GigaChatSettings(
            model = settings.model.takeIf { it.isNotEmpty() },
            temperature = if (settings.hasTemperature()) settings.temperature else null,
            topP = if (settings.hasTopP()) settings.topP else null,
            repetitionPenalty = if (settings.hasRepetitionPenalty()) settings.repetitionPenalty else null,
            updateInterval = if (settings.hasUpdateInterval()) settings.updateInterval else null,
            profanityCheck = if (settings.hasProfanityCheck()) settings.profanityCheck else null,
            filtersSettings = settings.filtersSettingsMap.mapValues { toDomainFilterSettings(it.value) },
            functions = settings.functionsList.map { toDomainFunction(it) },
            functionRegistry = if (settings.hasFunctionRegistry()) {
                toDomainFunctionRegistry(settings.functionRegistry)
            } else {
                null
            },
            filterStubPhrases = settings.filterStubPhrasesList,
            currentTime = if (settings.hasCurrentTime()) settings.currentTime else null,
            functionRanker = if (settings.hasFunctionRanker()) {
                toDomainFunctionRanker(settings.functionRanker)
            } else {
                null
            },
            preset = if (settings.hasPreset()) settings.preset else null,
        )

    private fun toDomainFunctionRanker(proto: ProtoFunctionRanker): DomainFunctionRanker =
        DomainFunctionRanker(
            enabled = if (proto.hasEnabled()) proto.enabled else null,
            topN = if (proto.hasTopN()) proto.topN.toInt() else null,
            embedderModel = if (proto.hasEmbedderModel()) proto.embedderModel else null,
            ignoredFunctions = proto.ignoredFunctionsList,
        )

    private fun toDomainFilterSettings(settings: ProtoFilterSettings): FilterSettings = FilterSettings(
        requestContent = if (settings.hasRequestContent()) {
            toDomainRequestContentSettings(settings.requestContent)
        } else {
            null
        },
        responseContent = if (settings.hasResponseContent()) {
            toDomainResponseContentSettings(settings.responseContent)
        } else {
            null
        }
    )

    private fun toDomainRequestContentSettings(
        settings: ProtoRequestContentSettings
    ): RequestContentSettings = RequestContentSettings(
        neuro = if (settings.hasNeuro()) settings.neuro else null,
        blacklist = if (settings.hasBlacklist()) settings.blacklist else null,
        whitelist = if (settings.hasWhitelist()) settings.whitelist else null
    )

    private fun toDomainResponseContentSettings(
        settings: ProtoResponseContentSettings
    ): ResponseContentSettings = ResponseContentSettings(
        blacklist = if (settings.hasBlacklist()) settings.blacklist else null
    )

    private fun toDomainFunction(function: ProtoFunction): FunctionDefinition = FunctionDefinition(
        name = function.name,
        description = function.description.takeIf { it.isNotEmpty() },
        parameters = function.parameters.takeIf { it.isNotEmpty() },
        fewShotExamples = function.fewShotExamplesList.map { toDomainFunctionExample(it) },
        returnParameters = function.returnParameters.takeIf { it.isNotEmpty() }
    )

    private fun toDomainFunctionExample(example: AnyExample): FunctionExample = FunctionExample(
        request = example.request,
        params = example.params.pairsList.map { it.key to it.value }
    )

    private fun toDomainFunctionRegistry(registry: ProtoFunctionRegistry): FunctionRegistry =
        FunctionRegistry(
            profile = registry.profile.takeIf { it.isNotEmpty() },
            labels = registry.labelsList,
            abFlags = registry.abFlags.takeIf { it.isNotEmpty() }
        )

    private fun toDomainInitialContext(context: ProtoInitialContext): InitialContext = InitialContext(
        messages = context.messagesList.map { toDomainMessage(it) }
    )

    private fun toDomainMessage(message: Message): DomainMessage = DomainMessage(
        role = message.role,
        content = message.content,
        functionCall = if (message.hasFunctionCall()) toDomainFunctionCall(message.functionCall) else null,
        functionName = message.functionName.takeIf { it.isNotEmpty() },
        functionsStateId = message.functionsStateId.takeIf { it.isNotEmpty() },
        attachments = message.attachmentsList,
        inlineData = message.inlineDataMap,
        functions = message.functionsList.map { toDomainFunction(it) }
    )

    private fun toDomainFunctionCall(call: FunctionCall): DomainFunctionCall = DomainFunctionCall(
        name = call.name,
        arguments = call.arguments
    )

    private fun toDomainFirstSpeaker(speaker: FirstSpeaker): DomainFirstSpeaker = DomainFirstSpeaker(
        type = speaker.type.takeIf { it.isNotEmpty() },
        lockFirstIn = if (speaker.hasLockFirstIn()) speaker.lockFirstIn else null
    )

    private fun toDomainVoiceMode(mode: Settings.Mode): VoiceMode = when (mode) {
        Settings.Mode.MODE_UNSPECIFIED, Settings.Mode.UNRECOGNIZED -> VoiceMode.UNSPECIFIED
        Settings.Mode.RECOGNIZE_GIGACHAT_SYNTHESIS -> VoiceMode.RECOGNIZE_GIGACHAT_SYNTHESIS
        Settings.Mode.GIGACHAT_SYNTHESIS -> VoiceMode.GIGACHAT_SYNTHESIS
        Settings.Mode.GIGACHAT -> VoiceMode.GIGACHAT
        Settings.Mode.RECOGNIZE_SYNTHESIS -> VoiceMode.RECOGNIZE_SYNTHESIS
    }

    private fun toDomainOutputModalities(modalities: Settings.OutputModalities): OutputModalities =
        when (modalities) {
            Settings.OutputModalities.MODALITIES_UNSPECIFIED, Settings.OutputModalities.UNRECOGNIZED ->
                OutputModalities.UNSPECIFIED
            Settings.OutputModalities.AUDIO -> OutputModalities.AUDIO
            Settings.OutputModalities.AUDIO_TEXT -> OutputModalities.AUDIO_TEXT
            Settings.OutputModalities.TEXT -> OutputModalities.TEXT
        }

    private fun toDomainDisableInterruption(disableInterruption: ProtoDisableInterruption): DisableInterruption =
        DisableInterruption(
            functions = disableInterruption.functionsList.map { toDomainLockFunctionExecution(it) }
        )

    private fun toDomainLockFunctionExecution(function: ProtoLockFunctionExecution): LockFunctionExecution =
        LockFunctionExecution(
            name = function.name,
            onExecution = function.onExecution,
            afterResult = function.afterResult
        )

    private fun toDomainContentFromClient(content: ContentFromClient): VoiceRequest =
        when (content.contentCase) {
            ContentFromClient.ContentCase.AUDIO_CONTENT ->
                VoiceRequest.Audio(toDomainAudioContent(content.audioContent))
            ContentFromClient.ContentCase.CONTENT_FOR_SYNTHESIS ->
                VoiceRequest.TextForSynthesis(toDomainSynthesisContent(content.contentForSynthesis))
            ContentFromClient.ContentCase.CONTENT_NOT_SET, null ->
                error("ContentFromClient content not set")
        }

    private fun toDomainAudioContent(audio: AudioContent): DomainAudioContent =
        DomainAudioContent(
            audioChunk = audio.audioChunk.takeIf { !it.isEmpty }?.let {
                ProtoTypeConverters.byteStringToByteArray(it)
            },
            speechStart = audio.speechStart,
            speechEnd = audio.speechEnd,
            meta = if (audio.hasMeta()) AudioChunkMeta(forceNoSpeech = audio.meta.forceNoSpeech) else null
        )

    private fun toDomainSynthesisContent(content: ContentForSynthesis): SynthesisContent =
        SynthesisContent(
            text = content.text,
            contentType = when (content.contentType) {
                ContentForSynthesis.ContentType.TEXT, ContentForSynthesis.ContentType.UNRECOGNIZED ->
                    SynthesisContentType.TEXT
                ContentForSynthesis.ContentType.SSML -> SynthesisContentType.SSML
            },
            isFinal = content.isFinal
        )

    private fun toDomainFunctionResult(result: FunctionResult): FunctionResultData = FunctionResultData(
        content = result.content,
        functionName = result.functionName.takeIf { it.isNotEmpty() }
    )

    private fun toDomainContext(context: ru.sbrf.dab2c.executor.clients.gigavoice.proto.Context): ContextData =
        ContextData(content = context.content)

    // ==================== Response Mapping: Domain -> Proto (Server-side) ====================

    /** Converts domain VoiceResponse to IVR proto response. */
    fun toProtoResponse(response: VoiceResponse): GigaVoiceResponse = when (response) {
        is VoiceResponse.Output ->
            gigaVoiceResponse { output = toProtoContentFromModel(response.content) }
        is VoiceResponse.FunctionCalling ->
            gigaVoiceResponse { functionCall = toProtoFunctionCalling(response.data) }
        is VoiceResponse.InputTranscription ->
            gigaVoiceResponse { inputTranscription = toProtoInputTranscription(response.transcription) }
        is VoiceResponse.OutputTranscription ->
            gigaVoiceResponse { outputTranscription = toProtoOutputTranscription(response.transcription) }
        is VoiceResponse.Error ->
            gigaVoiceResponse { error = toProtoError(response.error) }
        is VoiceResponse.Warning ->
            gigaVoiceResponse { warning = toProtoWarning(response.warning) }
        is VoiceResponse.InputFiles ->
            gigaVoiceResponse { inputFiles = toProtoInputFiles(response.data) }
        is VoiceResponse.PlatformFunctionProcessing ->
            gigaVoiceResponse { platformFunctionProcessing = toProtoPlatformFunctionProcessing(response.data) }
        is VoiceResponse.ServiceInfo ->
            gigaVoiceResponse { serviceInfo = toProtoServiceInfo(response.data) }
    }

    private fun toProtoServiceInfo(data: ServiceInfoData): ProtoServiceInfo =
        serviceInfo {
            services.addAll(
                data.services.map {
                    serviceVersion {
                        serviceName = it.serviceName
                        version = it.version
                        it.build?.let { build = it }
                    }
                }
            )
        }

    private fun toProtoPlatformFunctionProcessing(data: PlatformFunctionProcessingData):
        ProtoPlatformFunctionProcessing = platformFunctionProcessing {
        name = data.name
        timestamp = data.timestamp
    }

    private fun toProtoInputFiles(data: InputFilesData): ru.sbrf.dab2c.executor.clients.gigavoice.proto.InputFiles =
        inputFiles {
            files.addAll(
                data.files.map {
                    file {
                        id = it.id
                        type = it.type
                    }
                }
            )
        }

    private fun toProtoContentFromModel(content: DomainContentFromModel): ContentFromModel =
        when (content) {
            is DomainContentFromModel.Audio ->
                contentFromModel { audio = toProtoAudio(content.audio) }
            is DomainContentFromModel.AdditionalData ->
                contentFromModel { additionalData = toProtoAdditionalData(content.data) }
            is DomainContentFromModel.Interrupted ->
                contentFromModel { interrupted = true }
        }

    private fun toProtoAudio(audio: AudioOutput): Audio = audio {
        audioChunk = ProtoTypeConverters.byteArrayToByteString(audio.audioChunk)
        audio.audioDuration?.let { audioDuration = ProtoTypeConverters.kotlinDurationToProtoDuration(it) }
        audio.isFinal?.let { isFinal = it }
    }

    private fun toProtoAdditionalData(data: AdditionalDataContent): AdditionalData = additionalData {
        data.usage?.let { usage = toProtoUsage(it) }
        data.gigachatModelInfo?.let { gigachatModelInfo = toProtoGigaChatModelInfo(it) }
        data.finishReason?.let { finishReason = it }
    }

    private fun toProtoUsage(usage: UsageData): Usage = usage {
        promptTokens = usage.promptTokens
        completionTokens = usage.completionTokens
        totalTokens = usage.totalTokens
        precachedPromptTokens = usage.precachedPromptTokens
    }

    private fun toProtoGigaChatModelInfo(info: DomainGigaChatModelInfo): GigaChatModelInfo =
        gigaChatModelInfo {
            name = info.name
            version = info.version
        }

    private fun toProtoFunctionCalling(data: FunctionCallingData): FunctionCalling = functionCalling {
        functionCall = toProtoFunctionCall(data.functionCall)
        timestamp = data.timestamp
    }

    private fun toProtoInputTranscription(data: InputTranscriptionData): InputTranscription =
        inputTranscription {
            text = data.text
            timestamp = data.timestamp
            data.unnormalizedText?.let { unnormalizedText = it }
            data.personIdentity?.let { personIdentity = toProtoPersonIdentity(it) }
            data.prefetch?.let { prefetch = it }
            data.whisper?.let { whisper = it }
            data.emotion?.let { emotion = toProtoEmotion(it) }
        }

    private fun toProtoPersonIdentity(identity: DomainPersonIdentity): PersonIdentity = personIdentity {
        age = toProtoAgeType(identity.age)
        gender = toProtoGenderType(identity.gender)
        ageScore = identity.ageScore
        genderScore = identity.genderScore
    }

    private fun toProtoAgeType(type: ru.sbrf.dab2c.executor.domain.voice.AgeType): AgeType = when (type) {
        ru.sbrf.dab2c.executor.domain.voice.AgeType.NONE -> AgeType.AGE_NONE
        ru.sbrf.dab2c.executor.domain.voice.AgeType.CHILD -> AgeType.CHILD
        ru.sbrf.dab2c.executor.domain.voice.AgeType.ADULT -> AgeType.ADULT
    }

    private fun toProtoGenderType(type: ru.sbrf.dab2c.executor.domain.voice.GenderType): GenderType =
        when (type) {
            ru.sbrf.dab2c.executor.domain.voice.GenderType.NONE -> GenderType.GENDER_NONE
            ru.sbrf.dab2c.executor.domain.voice.GenderType.MALE -> GenderType.MALE
            ru.sbrf.dab2c.executor.domain.voice.GenderType.FEMALE -> GenderType.FEMALE
        }

    private fun toProtoEmotion(emotion: DomainEmotion): Emotion = emotion {
        positive = emotion.positive
        neutral = emotion.neutral
        negative = emotion.negative
    }

    private fun toProtoOutputTranscription(data: OutputTranscriptionData): OutputTranscription =
        outputTranscription {
            text = data.text
            functionsStateId = data.functionsStateId
            finishReason = data.finishReason
            timestamp = data.timestamp
            data.stubText?.let { stubText = it }
            inlineData.putAll(data.inlineData)
            data.silencePhrase?.let { silencePhrase = it }
        }

    private fun toProtoError(data: ErrorData): Error = error {
        status = data.status
        message = data.message
    }

    private fun toProtoWarning(data: WarningData): Warning = warning {
        message = data.message
    }

    private fun toProtoFunctionCall(call: DomainFunctionCall): FunctionCall = functionCall {
        name = call.name
        arguments = call.arguments
    }
}
