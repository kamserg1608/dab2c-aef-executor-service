package ru.sbrf.dab2c.executor.clients.ivr.mapper

import ru.sbrf.dab2c.executor.clients.converter.ProtoTypeConverters
import ru.sbrf.dab2c.executor.clients.ivr.proto.*
import ru.sbrf.dab2c.executor.domain.voice.AdditionalDataContent
import ru.sbrf.dab2c.executor.domain.voice.AudioContent
import ru.sbrf.dab2c.executor.domain.voice.AudioEncoding
import ru.sbrf.dab2c.executor.domain.voice.AudioInputSettings
import ru.sbrf.dab2c.executor.domain.voice.AudioOutput
import ru.sbrf.dab2c.executor.domain.voice.AudioOutputSettings
import ru.sbrf.dab2c.executor.domain.voice.AudioSettings
import ru.sbrf.dab2c.executor.domain.voice.ErrorData
import ru.sbrf.dab2c.executor.domain.voice.FilterSettings
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.FunctionDefinition
import ru.sbrf.dab2c.executor.domain.voice.FunctionExample
import ru.sbrf.dab2c.executor.domain.voice.FunctionRegistry
import ru.sbrf.dab2c.executor.domain.voice.FunctionResultData
import ru.sbrf.dab2c.executor.domain.voice.GigaChatSettings
import ru.sbrf.dab2c.executor.domain.voice.InitialContext
import ru.sbrf.dab2c.executor.domain.voice.InputTranscriptionData
import ru.sbrf.dab2c.executor.domain.voice.OutputModalities
import ru.sbrf.dab2c.executor.domain.voice.OutputTranscriptionData
import ru.sbrf.dab2c.executor.domain.voice.RequestContentSettings
import ru.sbrf.dab2c.executor.domain.voice.ResponseContentSettings
import ru.sbrf.dab2c.executor.domain.voice.SynthesisContent
import ru.sbrf.dab2c.executor.domain.voice.SynthesisContentType
import ru.sbrf.dab2c.executor.domain.voice.UsageData
import ru.sbrf.dab2c.executor.domain.voice.VoiceMode
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.domain.voice.WarningData
import ru.sbrf.dab2c.executor.domain.voice.ContentFromModel as DomainContentFromModel
import ru.sbrf.dab2c.executor.domain.voice.Emotion as DomainEmotion
import ru.sbrf.dab2c.executor.domain.voice.FirstSpeaker as DomainFirstSpeaker
import ru.sbrf.dab2c.executor.domain.voice.FunctionCall as DomainFunctionCall
import ru.sbrf.dab2c.executor.domain.voice.GigaChatModelInfo as DomainGigaChatModelInfo
import ru.sbrf.dab2c.executor.domain.voice.Message as DomainMessage
import ru.sbrf.dab2c.executor.domain.voice.PersonIdentity as DomainPersonIdentity

/**
 * Mapper for IVR proto <-> domain type conversions.
 */
object IvrDomainMapper {

    // ==================== Response Mapping: Proto -> Domain ====================

    fun toDomainResponse(response: IvrResponse): VoiceResponse =
        when (response.responseCase) {
            IvrResponse.ResponseCase.OUTPUT -> VoiceResponse.Output(toContentFromModel(response.output))
            IvrResponse.ResponseCase.FUNCTION_CALL -> VoiceResponse.FunctionCalling(toFunctionCallingData(response.functionCall))
            IvrResponse.ResponseCase.INPUT_TRANSCRIPTION -> VoiceResponse.InputTranscription(toInputTranscriptionData(response.inputTranscription))
            IvrResponse.ResponseCase.OUTPUT_TRANSCRIPTION -> VoiceResponse.OutputTranscription(toOutputTranscriptionData(response.outputTranscription))
            IvrResponse.ResponseCase.ERROR -> VoiceResponse.Error(toErrorData(response.error))
            IvrResponse.ResponseCase.WARNING -> VoiceResponse.Warning(toWarningData(response.warning))
            IvrResponse.ResponseCase.RESPONSE_NOT_SET, null -> error("Response not set")
        }

    private fun toContentFromModel(content: ContentFromModel): DomainContentFromModel =
        when (content.responseCase) {
            ContentFromModel.ResponseCase.AUDIO -> DomainContentFromModel.Audio(toAudioOutput(content.audio))
            ContentFromModel.ResponseCase.ADDITIONAL_DATA -> DomainContentFromModel.AdditionalData(toAdditionalDataContent(content.additionalData))
            ContentFromModel.ResponseCase.INTERRUPTED -> DomainContentFromModel.Interrupted
            ContentFromModel.ResponseCase.RESPONSE_NOT_SET, null -> error("ContentFromModel response not set")
        }

    private fun toAudioOutput(audio: Audio): AudioOutput = AudioOutput(
        audioChunk = ProtoTypeConverters.byteStringToByteArray(audio.audioChunk),
        audioDuration = if (audio.hasAudioDuration()) ProtoTypeConverters.protoDurationToKotlinDuration(audio.audioDuration) else null,
        isFinal = audio.isFinal
    )

    private fun toAdditionalDataContent(data: AdditionalData): AdditionalDataContent = AdditionalDataContent(
        usage = if (data.hasUsage()) toUsageData(data.usage) else null,
        gigachatModelInfo = if (data.hasGigachatModelInfo()) toGigaChatModelInfo(data.gigachatModelInfo) else null,
        finishReason = data.finishReason.takeIf { it.isNotEmpty() }
    )

    private fun toUsageData(usage: Usage): UsageData = UsageData(
        promptTokens = usage.promptTokens,
        completionTokens = usage.completionTokens,
        totalTokens = usage.totalTokens,
        precachedPromptTokens = usage.precachedPromptTokens
    )

    private fun toGigaChatModelInfo(info: GigaChatModelInfo): DomainGigaChatModelInfo = DomainGigaChatModelInfo(
        name = info.name,
        version = info.version
    )

    private fun toFunctionCallingData(data: FunctionCalling): FunctionCallingData = FunctionCallingData(
        functionCall = toFunctionCall(data.functionCall),
        timestamp = data.timestamp
    )

    private fun toFunctionCall(call: FunctionCall): DomainFunctionCall = DomainFunctionCall(
        name = call.name,
        arguments = call.arguments
    )

    private fun toInputTranscriptionData(transcription: InputTranscription): InputTranscriptionData = InputTranscriptionData(
        text = transcription.text,
        timestamp = transcription.timestamp,
        unnormalizedText = transcription.unnormalizedText.takeIf { it.isNotEmpty() },
        personIdentity = if (transcription.hasPersonIdentity()) toPersonIdentity(transcription.personIdentity) else null,
        prefetch = transcription.prefetch,
        whisper = transcription.whisper,
        emotion = if (transcription.hasEmotion()) toEmotion(transcription.emotion) else null
    )

    private fun toPersonIdentity(identity: PersonIdentity): DomainPersonIdentity = DomainPersonIdentity(
        age = toAgeType(identity.age),
        gender = toGenderType(identity.gender),
        ageScore = identity.ageScore,
        genderScore = identity.genderScore
    )

    private fun toAgeType(type: AgeType): ru.sbrf.dab2c.executor.domain.voice.AgeType = when (type) {
        AgeType.AGE_NONE, AgeType.UNRECOGNIZED -> ru.sbrf.dab2c.executor.domain.voice.AgeType.NONE
        AgeType.CHILD -> ru.sbrf.dab2c.executor.domain.voice.AgeType.CHILD
        AgeType.ADULT -> ru.sbrf.dab2c.executor.domain.voice.AgeType.ADULT
    }

    private fun toGenderType(type: GenderType): ru.sbrf.dab2c.executor.domain.voice.GenderType = when (type) {
        GenderType.GENDER_NONE, GenderType.UNRECOGNIZED -> ru.sbrf.dab2c.executor.domain.voice.GenderType.NONE
        GenderType.MALE -> ru.sbrf.dab2c.executor.domain.voice.GenderType.MALE
        GenderType.FEMALE -> ru.sbrf.dab2c.executor.domain.voice.GenderType.FEMALE
    }

    private fun toEmotion(emotion: Emotion): DomainEmotion = DomainEmotion(
        positive = emotion.positive,
        neutral = emotion.neutral,
        negative = emotion.negative
    )

    private fun toOutputTranscriptionData(transcription: OutputTranscription): OutputTranscriptionData = OutputTranscriptionData(
        text = transcription.text,
        functionsStateId = transcription.functionsStateId,
        finishReason = transcription.finishReason,
        timestamp = transcription.timestamp
    )

    private fun toErrorData(error: Error): ErrorData = ErrorData(
        status = error.status,
        message = error.message
    )

    private fun toWarningData(warning: Warning): WarningData = WarningData(
        message = warning.message
    )

    // ==================== Request Mapping: Domain -> Proto ====================

    fun toProtoRequest(request: VoiceRequest): IvrRequest = when (request) {
        is VoiceRequest.Settings -> ivrRequest { settings = toProtoSettings(request.settings) }
        is VoiceRequest.Audio -> ivrRequest { input = toContentFromClient(request.content) }
        is VoiceRequest.TextForSynthesis -> ivrRequest { input = toContentFromClientSynthesis(request.content) }
        is VoiceRequest.FunctionResult -> ivrRequest { functionResult = toProtoFunctionResult(request.result) }
    }

    private fun toContentFromClient(audio: AudioContent): ContentFromClient = contentFromClient {
        audioContent = audioContent {
            audio.audioChunk?.let { audioChunk = ProtoTypeConverters.byteArrayToByteString(it) }
            speechStart = audio.speechStart
            speechEnd = audio.speechEnd
        }
    }

    private fun toContentFromClientSynthesis(content: SynthesisContent): ContentFromClient = contentFromClient {
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
    }

    private fun toProtoAudioSettings(domainSettings: AudioSettings): ru.sbrf.dab2c.executor.clients.ivr.proto.AudioSettings = audioSettings {
        domainSettings.input?.let { input = toProtoInput(it) }
        domainSettings.output?.let { output = toProtoOutput(it) }
    }

    private fun toProtoInput(domainInput: AudioInputSettings): Input = input {
        domainInput.model?.let { model = it }
        audioEncoding = toProtoInputAudioEncoding(domainInput.audioEncoding)
        domainInput.sampleRate?.let { sampleRate = it }
        silencePhrases.addAll(domainInput.silencePhrases)
        domainInput.silencePhrasesTimeout?.let { silencePhrasesTimeout = ProtoTypeConverters.kotlinDurationToProtoDuration(it) }
        domainInput.silenceTimeout?.let { silenceTimeout = ProtoTypeConverters.kotlinDurationToProtoDuration(it) }
        stopPhrases.addAll(domainInput.stopPhrases)
        ignorePhrases.addAll(domainInput.ignorePhrases)
    }

    private fun toProtoOutput(domainOutput: AudioOutputSettings): Output = output {
        domainOutput.voice?.let { voice = it }
        audioEncoding = toProtoOutputAudioEncoding(domainOutput.audioEncoding)
    }

    private fun toProtoInputAudioEncoding(encoding: AudioEncoding): Input.AudioEncoding = when (encoding) {
        AudioEncoding.UNSPECIFIED -> Input.AudioEncoding.AUDIO_ENCODING_UNSPECIFIED
        AudioEncoding.PCM_S16LE -> Input.AudioEncoding.PCM_S16LE
        AudioEncoding.OPUS -> Input.AudioEncoding.OPUS
        AudioEncoding.PCM_ALAW -> Input.AudioEncoding.PCM_ALAW
    }

    private fun toProtoOutputAudioEncoding(encoding: AudioEncoding): Output.AudioEncoding = when (encoding) {
        AudioEncoding.UNSPECIFIED -> Output.AudioEncoding.AUDIO_ENCODING_UNSPECIFIED
        AudioEncoding.PCM_S16LE -> Output.AudioEncoding.PCM_S16LE
        AudioEncoding.OPUS -> Output.AudioEncoding.OPUS
        AudioEncoding.PCM_ALAW -> Output.AudioEncoding.PCM_ALAW
    }

    private fun toProtoGigaChatSettings(domainSettings: GigaChatSettings): ru.sbrf.dab2c.executor.clients.ivr.proto.GigaChatSettings = gigaChatSettings {
        domainSettings.model?.let { model = it }
        domainSettings.temperature?.let { temperature = it }
        domainSettings.topP?.let { topP = it }
        domainSettings.repetitionPenalty?.let { repetitionPenalty = it }
        domainSettings.updateInterval?.let { updateInterval = it }
        domainSettings.profanityCheck?.let { profanityCheck = it }
        filtersSettings.putAll(domainSettings.filtersSettings.mapValues { toProtoFilterSettings(it.value) })
        functions.addAll(domainSettings.functions.map { toProtoFunction(it) })
        domainSettings.functionRegistry?.let { functionRegistry = toProtoFunctionRegistry(it) }
    }

    private fun toProtoFilterSettings(domainSettings: FilterSettings): ru.sbrf.dab2c.executor.clients.ivr.proto.FilterSettings = filterSettings {
        domainSettings.requestContent?.let { requestContent = toProtoRequestContentSettings(it) }
        domainSettings.responseContent?.let { responseContent = toProtoResponseContentSettings(it) }
    }

    private fun toProtoRequestContentSettings(domainSettings: RequestContentSettings): ru.sbrf.dab2c.executor.clients.ivr.proto.RequestContentSettings = requestContentSettings {
        domainSettings.neuro?.let { neuro = it }
        domainSettings.blacklist?.let { blacklist = it }
        domainSettings.whitelist?.let { whitelist = it }
    }

    private fun toProtoResponseContentSettings(domainSettings: ResponseContentSettings): ru.sbrf.dab2c.executor.clients.ivr.proto.ResponseContentSettings = responseContentSettings {
        domainSettings.blacklist?.let { blacklist = it }
    }

    private fun toProtoFunction(domainFunction: FunctionDefinition): ru.sbrf.dab2c.executor.clients.ivr.proto.Function = function {
        name = domainFunction.name
        domainFunction.description?.let { description = it }
        domainFunction.parameters?.let { parameters = it }
        fewShotExamples.addAll(domainFunction.fewShotExamples.map { toProtoAnyExample(it) })
        domainFunction.returnParameters?.let { returnParameters = it }
    }

    private fun toProtoAnyExample(example: FunctionExample): AnyExample = anyExample {
        request = example.request
        params = params {
            pairs.addAll(example.params.map { (k, v) ->
                pair {
                    key = k
                    value = v
                }
            })
        }
    }

    private fun toProtoFunctionRegistry(domainRegistry: FunctionRegistry): ru.sbrf.dab2c.executor.clients.ivr.proto.FunctionRegistry = functionRegistry {
        domainRegistry.profile?.let { profile = it }
        labels.addAll(domainRegistry.labels)
        domainRegistry.abFlags?.let { abFlags = it }
    }

    private fun toProtoInitialContext(domainContext: InitialContext): ru.sbrf.dab2c.executor.clients.ivr.proto.InitialContext = initialContext {
        messages.addAll(domainContext.messages.map { toProtoMessage(it) })
    }

    private fun toProtoMessage(domainMessage: DomainMessage): Message = message {
        role = domainMessage.role
        content = domainMessage.content
        domainMessage.functionCall?.let { functionCall = toProtoFunctionCall(it) }
        domainMessage.functionName?.let { functionName = it }
        domainMessage.functionsStateId?.let { functionsStateId = it }
        attachments.addAll(domainMessage.attachments)
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

    private fun toProtoOutputModalities(modalities: OutputModalities): Settings.OutputModalities = when (modalities) {
        OutputModalities.UNSPECIFIED -> Settings.OutputModalities.MODALITIES_UNSPECIFIED
        OutputModalities.AUDIO -> Settings.OutputModalities.AUDIO
        OutputModalities.AUDIO_TEXT -> Settings.OutputModalities.AUDIO_TEXT
        OutputModalities.TEXT -> Settings.OutputModalities.TEXT
    }
}
