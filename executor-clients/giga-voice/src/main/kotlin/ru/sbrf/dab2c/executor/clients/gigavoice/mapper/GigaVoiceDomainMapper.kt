package ru.sbrf.dab2c.executor.clients.gigavoice.mapper

import ru.sbrf.dab2c.executor.clients.converter.ProtoTypeConverters
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.AdditionalData
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.AgeType
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.AnyExample
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Audio
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
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.InputFiles
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.InputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Message
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Output
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.OutputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.PersonIdentity
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.PlatformFunctionProcessing
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.ServiceInfo
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Usage
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Warning
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.anyExample
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audioChunkMeta
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audioContent
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audioSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.contentForSynthesis
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.contentFromClient
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
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaVoiceRequest
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
import ru.sbrf.dab2c.executor.domain.voice.AdditionalDataContent
import ru.sbrf.dab2c.executor.domain.voice.AudioEncoding
import ru.sbrf.dab2c.executor.domain.voice.AudioInputSettings
import ru.sbrf.dab2c.executor.domain.voice.AudioOutput
import ru.sbrf.dab2c.executor.domain.voice.AudioOutputSettings
import ru.sbrf.dab2c.executor.domain.voice.AudioSettings
import ru.sbrf.dab2c.executor.domain.voice.DisableInterruption
import ru.sbrf.dab2c.executor.domain.voice.ErrorData
import ru.sbrf.dab2c.executor.domain.voice.FileData
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
import ru.sbrf.dab2c.executor.domain.voice.ServiceVersion
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
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionRegistry as ProtoFunctionRegistry
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionSoundRule as ProtoFunctionSoundRule
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaChatSettings as ProtoGigaChatSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.InitialContext as ProtoInitialContext
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.LockFunctionExecution as ProtoLockFunctionExecution
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.RequestContentSettings as ProtoRequestContentSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.ResponseContentSettings as ProtoResponseContentSettings
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
 * Mapper for GigaVoice proto <-> domain type conversions.
 */
object GigaVoiceDomainMapper {

    // ==================== Response Mapping: Proto -> Domain ====================

    /** Converts GigaVoice proto response to domain VoiceResponse. */
    fun toDomainResponse(response: GigaVoiceResponse): VoiceResponse =
        when (response.responseCase) {
            GigaVoiceResponse.ResponseCase.OUTPUT ->
                VoiceResponse.Output(toContentFromModel(response.output))
            GigaVoiceResponse.ResponseCase.FUNCTION_CALL ->
                VoiceResponse.FunctionCalling(toFunctionCallingData(response.functionCall))
            GigaVoiceResponse.ResponseCase.INPUT_TRANSCRIPTION ->
                VoiceResponse.InputTranscription(toInputTranscriptionData(response.inputTranscription))
            GigaVoiceResponse.ResponseCase.OUTPUT_TRANSCRIPTION ->
                VoiceResponse.OutputTranscription(toOutputTranscriptionData(response.outputTranscription))
            GigaVoiceResponse.ResponseCase.ERROR ->
                VoiceResponse.Error(toErrorData(response.error))
            GigaVoiceResponse.ResponseCase.WARNING ->
                VoiceResponse.Warning(toWarningData(response.warning))
            GigaVoiceResponse.ResponseCase.INPUT_FILES ->
                VoiceResponse.InputFiles(toInputFilesData(response.inputFiles))
            GigaVoiceResponse.ResponseCase.PLATFORM_FUNCTION_PROCESSING ->
                VoiceResponse.PlatformFunctionProcessing(
                    toPlatformFunctionProcessingData(response.platformFunctionProcessing)
                )
            GigaVoiceResponse.ResponseCase.SERVICE_INFO ->
                VoiceResponse.ServiceInfo(toServiceInfoData(response.serviceInfo))
            GigaVoiceResponse.ResponseCase.RESPONSE_NOT_SET, null ->
                error("Response not set")
        }

    private fun toServiceInfoData(data: ServiceInfo): ServiceInfoData = ServiceInfoData(
        services = data.servicesList.map {
            ServiceVersion(
                serviceName = it.serviceName,
                version = it.version,
                build = it.build.takeIf { it.isNotEmpty() }
            )
        }
    )

    private fun toPlatformFunctionProcessingData(data: PlatformFunctionProcessing): PlatformFunctionProcessingData =
        PlatformFunctionProcessingData(
            name = data.name,
            timestamp = data.timestamp,
        )

    private fun toContentFromModel(content: ContentFromModel): DomainContentFromModel =
        when (content.responseCase) {
            ContentFromModel.ResponseCase.AUDIO ->
                DomainContentFromModel.Audio(toAudioOutput(content.audio))
            ContentFromModel.ResponseCase.ADDITIONAL_DATA ->
                DomainContentFromModel.AdditionalData(toAdditionalDataContent(content.additionalData))
            ContentFromModel.ResponseCase.INTERRUPTED ->
                DomainContentFromModel.Interrupted
            ContentFromModel.ResponseCase.RESPONSE_NOT_SET, null ->
                error("ContentFromModel response not set")
        }

    private fun toAudioOutput(audio: Audio): AudioOutput = AudioOutput(
        audioChunk = ProtoTypeConverters.byteStringToByteArray(audio.audioChunk),
        audioDuration = if (audio.hasAudioDuration()) {
            ProtoTypeConverters.protoDurationToKotlinDuration(audio.audioDuration)
        } else {
            null
        },
        isFinal = audio.isFinal
    )

    private fun toAdditionalDataContent(data: AdditionalData): AdditionalDataContent =
        AdditionalDataContent(
            usage = if (data.hasUsage()) toUsageData(data.usage) else null,
            gigachatModelInfo = if (data.hasGigachatModelInfo()) {
                toGigaChatModelInfo(data.gigachatModelInfo)
            } else {
                null
            },
            finishReason = data.finishReason.takeIf { it.isNotEmpty() }
        )

    private fun toUsageData(usage: Usage): UsageData = UsageData(
        promptTokens = usage.promptTokens,
        completionTokens = usage.completionTokens,
        totalTokens = usage.totalTokens,
        precachedPromptTokens = usage.precachedPromptTokens
    )

    private fun toGigaChatModelInfo(info: GigaChatModelInfo): DomainGigaChatModelInfo =
        DomainGigaChatModelInfo(
            name = info.name,
            version = info.version
        )

    private fun toFunctionCallingData(data: FunctionCalling): FunctionCallingData =
        FunctionCallingData(
            functionCall = toFunctionCall(data.functionCall),
            timestamp = data.timestamp
        )

    private fun toFunctionCall(call: FunctionCall): DomainFunctionCall = DomainFunctionCall(
        name = call.name,
        arguments = call.arguments
    )

    private fun toInputTranscriptionData(transcription: InputTranscription): InputTranscriptionData =
        InputTranscriptionData(
            text = transcription.text,
            timestamp = transcription.timestamp,
            unnormalizedText = transcription.unnormalizedText.takeIf { it.isNotEmpty() },
            personIdentity = if (transcription.hasPersonIdentity()) {
                toPersonIdentity(transcription.personIdentity)
            } else {
                null
            },
            prefetch = transcription.prefetch,
            whisper = transcription.whisper,
            emotion = if (transcription.hasEmotion()) toEmotion(transcription.emotion) else null
        )

    private fun toPersonIdentity(identity: PersonIdentity): DomainPersonIdentity =
        DomainPersonIdentity(
            age = toAgeType(identity.age),
            gender = toGenderType(identity.gender),
            ageScore = identity.ageScore,
            genderScore = identity.genderScore
        )

    private fun toAgeType(type: AgeType): ru.sbrf.dab2c.executor.domain.voice.AgeType =
        when (type) {
            AgeType.AGE_NONE, AgeType.UNRECOGNIZED ->
                ru.sbrf.dab2c.executor.domain.voice.AgeType.NONE
            AgeType.CHILD -> ru.sbrf.dab2c.executor.domain.voice.AgeType.CHILD
            AgeType.ADULT -> ru.sbrf.dab2c.executor.domain.voice.AgeType.ADULT
        }

    private fun toGenderType(type: GenderType): ru.sbrf.dab2c.executor.domain.voice.GenderType =
        when (type) {
            GenderType.GENDER_NONE, GenderType.UNRECOGNIZED ->
                ru.sbrf.dab2c.executor.domain.voice.GenderType.NONE
            GenderType.MALE -> ru.sbrf.dab2c.executor.domain.voice.GenderType.MALE
            GenderType.FEMALE -> ru.sbrf.dab2c.executor.domain.voice.GenderType.FEMALE
        }

    private fun toEmotion(emotion: Emotion): DomainEmotion = DomainEmotion(
        positive = emotion.positive,
        neutral = emotion.neutral,
        negative = emotion.negative
    )

    private fun toOutputTranscriptionData(transcription: OutputTranscription): OutputTranscriptionData =
        OutputTranscriptionData(
            text = transcription.text,
            functionsStateId = transcription.functionsStateId,
            finishReason = transcription.finishReason,
            timestamp = transcription.timestamp,
            stubText = transcription.stubText.takeIf { it.isNotEmpty() },
            inlineData = transcription.inlineDataMap,
            silencePhrase = if (transcription.hasSilencePhrase()) transcription.silencePhrase else null
        )

    private fun toInputFilesData(inputFiles: InputFiles): InputFilesData = InputFilesData(
        files = inputFiles.filesList.map { FileData(id = it.id, type = it.type) }
    )

    private fun toErrorData(error: Error): ErrorData = ErrorData(
        status = error.status,
        message = error.message
    )

    private fun toWarningData(warning: Warning): WarningData = WarningData(
        message = warning.message
    )

    // ==================== Request Mapping: Domain -> Proto ====================

    /** Converts domain VoiceRequest to GigaVoice proto request. */
    fun toProtoRequest(request: VoiceRequest): GigaVoiceRequest = when (request) {
        is VoiceRequest.Settings ->
            gigaVoiceRequest { settings = toProtoSettings(request.settings) }
        is VoiceRequest.Audio ->
            gigaVoiceRequest { input = toContentFromClient(request.content) }
        is VoiceRequest.TextForSynthesis ->
            gigaVoiceRequest { input = toContentFromClientSynthesis(request.content) }
        is VoiceRequest.FunctionResult ->
            gigaVoiceRequest { functionResult = toProtoFunctionResult(request.result) }
        is VoiceRequest.Context ->
            error("Context requests are not forwarded to downstream GigaVoice service")
    }

    private fun toContentFromClient(audio: DomainAudioContent): ContentFromClient = contentFromClient {
        audioContent = audioContent {
            audio.audioChunk?.let { audioChunk = ProtoTypeConverters.byteArrayToByteString(it) }
            speechStart = audio.speechStart
            speechEnd = audio.speechEnd
            audio.meta?.let { meta = audioChunkMeta { forceNoSpeech = it.forceNoSpeech } }
        }
    }

    private fun toContentFromClientSynthesis(content: SynthesisContent): ContentFromClient =
        contentFromClient {
            contentForSynthesis = contentForSynthesis {
                text = content.text
                contentType = when (content.contentType) {
                    SynthesisContentType.TEXT -> ContentForSynthesis.ContentType.TEXT
                    SynthesisContentType.SSML -> ContentForSynthesis.ContentType.SSML
                }
                isFinal = content.isFinal
            }
        }

    private fun toProtoFunctionResult(result: FunctionResultData): FunctionResult = functionResult {
        content = result.content
        result.functionName?.let { functionName = it }
    }

    private fun toProtoSettings(domainSettings: VoiceSettings): Settings = settings {
        voiceCallId = domainSettings.voiceCallId
        audio = toProtoAudioSettings(domainSettings.audio)
        domainSettings.gigachat?.let { gigachat = toProtoGigaChatSettings(it) }
        domainSettings.context?.let { context = toProtoInitialContext(it) }
        disableVad = domainSettings.disableVad
        enableTranscribeInput = domainSettings.enableTranscribeInput
        flags.addAll(domainSettings.flags)
        outputModalities = toProtoOutputModalities(domainSettings.outputModalities)
        mode = toProtoVoiceMode(domainSettings.mode)
        domainSettings.firstSpeaker?.let { firstSpeaker = toProtoFirstSpeaker(it) }
        enableDenoiser = domainSettings.enableDenoiser
        enablePrefetch = domainSettings.enablePrefetch
        enablePersonIdentity = domainSettings.enablePersonIdentity
        enableWhisper = domainSettings.enableWhisper
        enableEmotion = domainSettings.enableEmotion
        enableTranscribeSilencePhrases = domainSettings.enableTranscribeSilencePhrases
        domainSettings.disableInterruption?.let { disableInterruption = toProtoDisableInterruption(it) }
    }

    private fun toProtoDisableInterruption(interruption: DisableInterruption): ProtoDisableInterruption =
        disableInterruption {
            functions.addAll(interruption.functions.map { toProtoLockFunctionExecution(it) })
        }

    private fun toProtoLockFunctionExecution(lock: LockFunctionExecution): ProtoLockFunctionExecution =
        lockFunctionExecution {
            name = lock.name
            onExecution = lock.onExecution
            afterResult = lock.afterResult
        }

    private fun toProtoAudioSettings(domainSettings: AudioSettings): ProtoAudioSettings =
        audioSettings {
            domainSettings.input?.let { input = toProtoInput(it) }
            domainSettings.output?.let { output = toProtoOutput(it) }
        }

    private fun toProtoInput(domainInput: AudioInputSettings): Input = input {
        domainInput.model?.let { model = it }
        audioEncoding = toProtoInputAudioEncoding(domainInput.audioEncoding)
        domainInput.sampleRate?.let { sampleRate = it }
        silencePhrases.addAll(domainInput.silencePhrases)
        domainInput.silencePhrasesTimeout?.let {
            silencePhrasesTimeout = ProtoTypeConverters.kotlinDurationToProtoDuration(it)
        }
        domainInput.silenceTimeout?.let {
            silenceTimeout = ProtoTypeConverters.kotlinDurationToProtoDuration(it)
        }
        stopPhrases.addAll(domainInput.stopPhrases)
        ignorePhrases.addAll(domainInput.ignorePhrases)
    }

    private fun toProtoOutput(domainOutput: AudioOutputSettings): Output = output {
        domainOutput.voice?.let { voice = it }
        audioEncoding = toProtoOutputAudioEncoding(domainOutput.audioEncoding)
        domainOutput.stubSounds?.let { stubSounds = toProtoStubSounds(it) }
        speed = toProtoSpeed(domainOutput.speed)
    }

    private fun toProtoStubSounds(
        domain: StubSounds
    ): ru.sbrf.dab2c.executor.clients.gigavoice.proto.StubSounds = stubSounds {
        domain.triggerGeneration?.let {
            triggerGeneration = triggerGeneration {
                it.timeout?.let { t -> timeout = ProtoTypeConverters.kotlinDurationToProtoDuration(t) }
                enable = it.enable
            }
        }
        domain.triggerFunction?.let {
            triggerFunction = triggerFunction {
                enable = it.enable
                mode = toProtoTriggerFunctionMode(it.mode)
                functionNames.addAll(it.functionNames)
                rules.addAll(it.rules.map { toProtoFunctionSoundRule(it) })
            }
        }
        sounds.addAll(domain.sounds)
    }

    private fun toProtoFunctionSoundRule(rule: FunctionSoundRule): ProtoFunctionSoundRule =
        functionSoundRule {
            functionNames.addAll(rule.functionNames)
            sounds.addAll(rule.sounds)
        }

    private fun toProtoTriggerFunctionMode(
        mode: TriggerFunctionMode
    ): ru.sbrf.dab2c.executor.clients.gigavoice.proto.TriggerFunction.Mode = when (mode) {
        TriggerFunctionMode.UNSPECIFIED ->
            ru.sbrf.dab2c.executor.clients.gigavoice.proto.TriggerFunction.Mode.MODE_UNSPECIFIED
        TriggerFunctionMode.WHITELIST ->
            ru.sbrf.dab2c.executor.clients.gigavoice.proto.TriggerFunction.Mode.WHITELIST
        TriggerFunctionMode.BLACKLIST ->
            ru.sbrf.dab2c.executor.clients.gigavoice.proto.TriggerFunction.Mode.BLACKLIST
    }

    private fun toProtoInputAudioEncoding(encoding: AudioEncoding): Input.AudioEncoding =
        when (encoding) {
            AudioEncoding.UNSPECIFIED -> Input.AudioEncoding.AUDIO_ENCODING_UNSPECIFIED
            AudioEncoding.PCM_S16LE -> Input.AudioEncoding.PCM_S16LE
            AudioEncoding.OPUS -> Input.AudioEncoding.OPUS
            AudioEncoding.PCM_ALAW -> Input.AudioEncoding.PCM_ALAW
        }

    private fun toProtoOutputAudioEncoding(encoding: AudioEncoding): Output.AudioEncoding =
        when (encoding) {
            AudioEncoding.UNSPECIFIED -> Output.AudioEncoding.AUDIO_ENCODING_UNSPECIFIED
            AudioEncoding.PCM_S16LE -> Output.AudioEncoding.PCM_S16LE
            AudioEncoding.OPUS -> Output.AudioEncoding.OPUS
            AudioEncoding.PCM_ALAW -> Output.AudioEncoding.PCM_ALAW
        }

    private fun toProtoSpeed(speed: Speed): Output.Speed =
        when (speed) {
            Speed.SPEED_UNSPECIFIED -> Output.Speed.SPEED_UNSPECIFIED
            Speed.EXTRA_SLOW -> Output.Speed.EXTRA_SLOW
            Speed.SLOW -> Output.Speed.SLOW
            Speed.MEDIUM -> Output.Speed.MEDIUM
            Speed.FAST -> Output.Speed.FAST
            Speed.EXTRA_FAST -> Output.Speed.EXTRA_FAST
        }

    private fun toProtoGigaChatSettings(domainSettings: GigaChatSettings): ProtoGigaChatSettings =
        gigaChatSettings {
            domainSettings.model?.let { model = it }
            domainSettings.temperature?.let { temperature = it }
            domainSettings.topP?.let { topP = it }
            domainSettings.repetitionPenalty?.let { repetitionPenalty = it }
            domainSettings.updateInterval?.let { updateInterval = it }
            domainSettings.profanityCheck?.let { profanityCheck = it }
            filtersSettings.putAll(
                domainSettings.filtersSettings.mapValues { toProtoFilterSettings(it.value) }
            )
            functions.addAll(domainSettings.functions.map { toProtoFunction(it) })
            domainSettings.functionRegistry?.let { functionRegistry = toProtoFunctionRegistry(it) }
            filterStubPhrases.addAll(domainSettings.filterStubPhrases)
            domainSettings.currentTime?.let { currentTime = it }
            domainSettings.functionRanker?.let { functionRanker = toProtoFunctionRanker(it) }
            domainSettings.preset?.let { preset = it }
        }

    private fun toProtoFunctionRanker(
        domain: DomainFunctionRanker
    ): ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionRanker =
        functionRanker {
            domain.enabled?.let { enabled = it }
            domain.topN?.let { topN = it }
            domain.embedderModel?.let { embedderModel = it }
            ignoredFunctions.addAll(domain.ignoredFunctions)
        }

    private fun toProtoFilterSettings(domainSettings: FilterSettings): ProtoFilterSettings =
        filterSettings {
            domainSettings.requestContent?.let { requestContent = toProtoRequestContentSettings(it) }
            domainSettings.responseContent?.let {
                responseContent = toProtoResponseContentSettings(it)
            }
        }

    private fun toProtoRequestContentSettings(
        domainSettings: RequestContentSettings
    ): ProtoRequestContentSettings = requestContentSettings {
        domainSettings.neuro?.let { neuro = it }
        domainSettings.blacklist?.let { blacklist = it }
        domainSettings.whitelist?.let { whitelist = it }
    }

    private fun toProtoResponseContentSettings(
        domainSettings: ResponseContentSettings
    ): ProtoResponseContentSettings = responseContentSettings {
        domainSettings.blacklist?.let { blacklist = it }
    }

    private fun toProtoFunction(domainFunction: FunctionDefinition): ProtoFunction = function {
        name = domainFunction.name
        domainFunction.description?.let { description = it }
        domainFunction.parameters?.let { parameters = it }
        fewShotExamples.addAll(domainFunction.fewShotExamples.map { toProtoAnyExample(it) })
        domainFunction.returnParameters?.let { returnParameters = it }
    }

    private fun toProtoAnyExample(example: FunctionExample): AnyExample = anyExample {
        request = example.request
        params = params {
            pairs.addAll(
                example.params.map { (k, v) ->
                    pair {
                        key = k
                        value = v
                    }
                }
            )
        }
    }

    private fun toProtoFunctionRegistry(domainRegistry: FunctionRegistry): ProtoFunctionRegistry =
        functionRegistry {
            domainRegistry.profile?.let { profile = it }
            labels.addAll(domainRegistry.labels)
            domainRegistry.abFlags?.let { abFlags = it }
        }

    private fun toProtoInitialContext(domainContext: InitialContext): ProtoInitialContext =
        initialContext {
            messages.addAll(domainContext.messages.map { toProtoMessage(it) })
        }

    private fun toProtoMessage(domainMessage: DomainMessage): Message = message {
        role = domainMessage.role
        content = domainMessage.content
        domainMessage.functionCall?.let { functionCall = toProtoFunctionCall(it) }
        domainMessage.functionName?.let { functionName = it }
        domainMessage.functionsStateId?.let { functionsStateId = it }
        attachments.addAll(domainMessage.attachments)
        inlineData.putAll(domainMessage.inlineData)
        functions.addAll(domainMessage.functions.map { toProtoFunction(it) })
    }

    private fun toProtoFunctionCall(call: DomainFunctionCall): FunctionCall = functionCall {
        name = call.name
        arguments = call.arguments
    }

    private fun toProtoFirstSpeaker(domainSpeaker: DomainFirstSpeaker): FirstSpeaker = firstSpeaker {
        domainSpeaker.type?.let { type = it }
        domainSpeaker.lockFirstIn?.let { lockFirstIn = it }
    }

    private fun toProtoVoiceMode(mode: VoiceMode): Settings.Mode = when (mode) {
        VoiceMode.UNSPECIFIED -> Settings.Mode.MODE_UNSPECIFIED
        VoiceMode.RECOGNIZE_GIGACHAT_SYNTHESIS -> Settings.Mode.RECOGNIZE_GIGACHAT_SYNTHESIS
        VoiceMode.GIGACHAT_SYNTHESIS -> Settings.Mode.GIGACHAT_SYNTHESIS
        VoiceMode.GIGACHAT -> Settings.Mode.GIGACHAT
        VoiceMode.RECOGNIZE_SYNTHESIS -> Settings.Mode.RECOGNIZE_SYNTHESIS
    }

    private fun toProtoOutputModalities(modalities: OutputModalities): Settings.OutputModalities =
        when (modalities) {
            OutputModalities.UNSPECIFIED -> Settings.OutputModalities.MODALITIES_UNSPECIFIED
            OutputModalities.AUDIO -> Settings.OutputModalities.AUDIO
            OutputModalities.AUDIO_TEXT -> Settings.OutputModalities.AUDIO_TEXT
            OutputModalities.TEXT -> Settings.OutputModalities.TEXT
        }
}
