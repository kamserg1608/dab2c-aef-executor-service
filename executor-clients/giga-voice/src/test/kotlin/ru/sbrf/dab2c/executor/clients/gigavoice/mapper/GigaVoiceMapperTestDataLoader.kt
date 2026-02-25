package ru.sbrf.dab2c.executor.clients.gigavoice.mapper

import com.fasterxml.jackson.databind.JsonNode
import com.google.protobuf.util.JsonFormat
import org.junit.jupiter.params.provider.Arguments
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.domain.voice.AdditionalDataContent
import ru.sbrf.dab2c.executor.domain.voice.AgeType
import ru.sbrf.dab2c.executor.domain.voice.AudioContent
import ru.sbrf.dab2c.executor.domain.voice.AudioEncoding
import ru.sbrf.dab2c.executor.domain.voice.AudioInputSettings
import ru.sbrf.dab2c.executor.domain.voice.AudioOutput
import ru.sbrf.dab2c.executor.domain.voice.AudioOutputSettings
import ru.sbrf.dab2c.executor.domain.voice.AudioSettings
import ru.sbrf.dab2c.executor.domain.voice.ContentFromModel
import ru.sbrf.dab2c.executor.domain.voice.Emotion
import ru.sbrf.dab2c.executor.domain.voice.ErrorData
import ru.sbrf.dab2c.executor.domain.voice.FilterSettings
import ru.sbrf.dab2c.executor.domain.voice.FirstSpeaker
import ru.sbrf.dab2c.executor.domain.voice.FunctionCall
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.FunctionDefinition
import ru.sbrf.dab2c.executor.domain.voice.FunctionExample
import ru.sbrf.dab2c.executor.domain.voice.FunctionRegistry
import ru.sbrf.dab2c.executor.domain.voice.FunctionResultData
import ru.sbrf.dab2c.executor.domain.voice.GenderType
import ru.sbrf.dab2c.executor.domain.voice.GigaChatModelInfo
import ru.sbrf.dab2c.executor.domain.voice.GigaChatSettings
import ru.sbrf.dab2c.executor.domain.voice.InitialContext
import ru.sbrf.dab2c.executor.domain.voice.InputTranscriptionData
import ru.sbrf.dab2c.executor.domain.voice.Message
import ru.sbrf.dab2c.executor.domain.voice.OutputModalities
import ru.sbrf.dab2c.executor.domain.voice.OutputTranscriptionData
import ru.sbrf.dab2c.executor.domain.voice.PersonIdentity
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
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import java.util.Base64
import java.util.stream.Stream
import kotlin.time.Duration.Companion.milliseconds

/**
 * Loads test data from JSON files for GigaVoiceDomainMapper tests.
 */
object GigaVoiceMapperTestDataLoader {
    private val protoJsonParser = JsonFormat.parser().ignoringUnknownFields()

    /**
     * Loads response test cases (Proto -> Domain).
     */
    fun loadResponseTestCases(): Stream<Arguments> {
        return loadTestCasesFromDirectory("mapper-test-data/response/")
            .map { (name, json) ->
                val protoResponse = parseProtoResponse(json["input"])
                val expectedDomain = parseDomainResponse(json["expected"])
                Arguments.of(name, protoResponse, expectedDomain)
            }
    }

    /**
     * Loads request test cases (Domain -> Proto).
     */
    fun loadRequestTestCases(): Stream<Arguments> {
        return loadTestCasesFromDirectory("mapper-test-data/request/")
            .map { (name, json) ->
                val domainRequest = parseDomainRequest(json["input"])
                val expectedProto = parseProtoRequest(json["expected"])
                Arguments.of(name, domainRequest, expectedProto)
            }
    }

    private fun loadTestCasesFromDirectory(directory: String): Stream<Pair<String, JsonNode>> {
        val classLoader = GigaVoiceMapperTestDataLoader::class.java.classLoader
        val resourcePath = classLoader.getResource(directory)
            ?: error("Test data directory not found: $directory")

        val files = java.io.File(resourcePath.toURI()).listFiles { file ->
            file.isFile && file.extension == "json"
        } ?: emptyArray()

        return files.map { file ->
            val json = ObjectMappers.MAPPER.readTree(file)
            val name = json["name"]?.asText() ?: file.nameWithoutExtension
            name to json
        }.stream()
    }

    // ===================== Proto Parsing =====================

    private fun parseProtoResponse(json: JsonNode): GigaVoiceResponse {
        val builder = GigaVoiceResponse.newBuilder()
        protoJsonParser.merge(json.toString(), builder)
        return builder.build()
    }

    private fun parseProtoRequest(json: JsonNode): GigaVoiceRequest {
        val builder = GigaVoiceRequest.newBuilder()
        protoJsonParser.merge(json.toString(), builder)
        return builder.build()
    }

    // ===================== Domain Parsing =====================

    private fun parseDomainResponse(json: JsonNode): VoiceResponse =
        when (val type = json["type"].asText()) {
            "Output" -> VoiceResponse.Output(parseContentFromModel(json["content"]))
            "FunctionCalling" -> VoiceResponse.FunctionCalling(parseFunctionCallingData(json["data"]))
            "InputTranscription" ->
                VoiceResponse.InputTranscription(parseInputTranscriptionData(json["transcription"]))
            "OutputTranscription" ->
                VoiceResponse.OutputTranscription(parseOutputTranscriptionData(json["transcription"]))
            "Error" -> VoiceResponse.Error(parseErrorData(json["error"]))
            "Warning" -> VoiceResponse.Warning(parseWarningData(json["warning"]))
            else -> error("Unknown VoiceResponse type: $type")
        }

    private fun parseContentFromModel(json: JsonNode): ContentFromModel {
        return when (val type = json["type"].asText()) {
            "Audio" -> ContentFromModel.Audio(parseAudioOutput(json["audio"]))
            "AdditionalData" -> ContentFromModel.AdditionalData(parseAdditionalDataContent(json["data"]))
            "Interrupted" -> ContentFromModel.Interrupted
            else -> error("Unknown ContentFromModel type: $type")
        }
    }

    private fun parseAudioOutput(json: JsonNode): AudioOutput {
        val audioChunkBase64 = json["audioChunk"]?.asText() ?: ""
        val audioChunk = if (audioChunkBase64.isNotEmpty()) {
            Base64.getDecoder().decode(audioChunkBase64)
        } else {
            byteArrayOf()
        }

        val audioDuration = json["audioDurationMs"]?.let { node ->
            if (node.isNull) null else node.asLong().milliseconds
        }

        return AudioOutput(
            audioChunk = audioChunk,
            audioDuration = audioDuration,
            isFinal = json["isFinal"]?.asBoolean() ?: false
        )
    }

    private fun parseAdditionalDataContent(json: JsonNode): AdditionalDataContent {
        return AdditionalDataContent(
            usage = json["usage"]?.takeIf { !it.isNull }?.let { parseUsageData(it) },
            gigachatModelInfo = json["gigachatModelInfo"]?.takeIf { !it.isNull }?.let { parseGigaChatModelInfo(it) },
            finishReason = json["finishReason"]?.takeIf { !it.isNull }?.asText()
        )
    }

    private fun parseUsageData(json: JsonNode): UsageData {
        return UsageData(
            promptTokens = json["promptTokens"].asInt(),
            completionTokens = json["completionTokens"].asInt(),
            totalTokens = json["totalTokens"].asInt(),
            precachedPromptTokens = json["precachedPromptTokens"]?.asInt() ?: 0
        )
    }

    private fun parseGigaChatModelInfo(json: JsonNode): GigaChatModelInfo {
        return GigaChatModelInfo(
            name = json["name"].asText(),
            version = json["version"].asText()
        )
    }

    private fun parseFunctionCallingData(json: JsonNode): FunctionCallingData {
        return FunctionCallingData(
            functionCall = parseFunctionCall(json["functionCall"]),
            timestamp = json["timestamp"].asLong()
        )
    }

    private fun parseFunctionCall(json: JsonNode): FunctionCall {
        return FunctionCall(
            name = json["name"].asText(),
            arguments = json["arguments"].asText()
        )
    }

    private fun parseInputTranscriptionData(json: JsonNode): InputTranscriptionData {
        return InputTranscriptionData(
            text = json["text"].asText(),
            timestamp = json["timestamp"].asLong(),
            unnormalizedText = json["unnormalizedText"]?.takeIf { !it.isNull }?.asText(),
            personIdentity = json["personIdentity"]?.takeIf { !it.isNull }?.let { parsePersonIdentity(it) },
            prefetch = json["prefetch"]?.asBoolean() ?: false,
            whisper = json["whisper"]?.asBoolean() ?: false,
            emotion = json["emotion"]?.takeIf { !it.isNull }?.let { parseEmotion(it) }
        )
    }

    private fun parsePersonIdentity(json: JsonNode): PersonIdentity {
        return PersonIdentity(
            age = AgeType.valueOf(json["age"].asText()),
            gender = GenderType.valueOf(json["gender"].asText()),
            ageScore = json["ageScore"].floatValue(),
            genderScore = json["genderScore"].floatValue()
        )
    }

    private fun parseEmotion(json: JsonNode): Emotion {
        return Emotion(
            positive = json["positive"].floatValue(),
            neutral = json["neutral"].floatValue(),
            negative = json["negative"].floatValue()
        )
    }

    private fun parseOutputTranscriptionData(json: JsonNode): OutputTranscriptionData {
        return OutputTranscriptionData(
            text = json["text"].asText(),
            functionsStateId = json["functionsStateId"].asText(),
            finishReason = json["finishReason"].asText(),
            timestamp = json["timestamp"].asLong()
        )
    }

    private fun parseErrorData(json: JsonNode): ErrorData {
        return ErrorData(
            status = json["status"].asInt(),
            message = json["message"].asText()
        )
    }

    private fun parseWarningData(json: JsonNode): WarningData {
        return WarningData(
            message = json["message"].asText()
        )
    }

    // ===================== Domain Request Parsing =====================

    private fun parseDomainRequest(json: JsonNode): VoiceRequest {
        return when (val type = json["type"].asText()) {
            "Settings" -> VoiceRequest.Settings(parseVoiceSettings(json["settings"]))
            "Audio" -> VoiceRequest.Audio(parseAudioContent(json["content"]))
            "TextForSynthesis" -> VoiceRequest.TextForSynthesis(parseSynthesisContent(json["content"]))
            "FunctionResult" -> VoiceRequest.FunctionResult(parseFunctionResultData(json["result"]))
            else -> error("Unknown VoiceRequest type: $type")
        }
    }

    private fun parseAudioContent(json: JsonNode): AudioContent {
        val audioChunkBase64 = json["audioChunk"]?.takeIf { !it.isNull }?.asText()
        val audioChunk = audioChunkBase64?.let { Base64.getDecoder().decode(it) }

        return AudioContent(
            audioChunk = audioChunk,
            speechStart = json["speechStart"]?.asBoolean() ?: false,
            speechEnd = json["speechEnd"]?.asBoolean() ?: false
        )
    }

    private fun parseSynthesisContent(json: JsonNode): SynthesisContent {
        return SynthesisContent(
            text = json["text"].asText(),
            contentType = SynthesisContentType.valueOf(json["contentType"]?.asText() ?: "TEXT"),
            isFinal = json["isFinal"]?.asBoolean() ?: false
        )
    }

    private fun parseFunctionResultData(json: JsonNode): FunctionResultData {
        return FunctionResultData(
            content = json["content"].asText(),
            functionName = json["functionName"]?.takeIf { !it.isNull }?.asText()
        )
    }

    private fun parseVoiceSettings(json: JsonNode): VoiceSettings {
        return VoiceSettings(
            voiceCallId = json["voiceCallId"].asText(),
            audio = parseAudioSettings(json["audio"]),
            gigachat = json["gigachat"]?.takeIf { !it.isNull }?.let { parseGigaChatSettings(it) },
            context = json["context"]?.takeIf { !it.isNull }?.let { parseInitialContext(it) },
            disableVad = json["disableVad"]?.asBoolean() ?: false,
            enableTranscribeInput = json["enableTranscribeInput"]?.asBoolean() ?: false,
            flags = json["flags"]?.map { it.asText() } ?: emptyList(),
            outputModalities = json["outputModalities"]?.asText()?.let { OutputModalities.valueOf(it) }
                ?: OutputModalities.UNSPECIFIED,
            mode = json["mode"]?.asText()?.let { VoiceMode.valueOf(it) } ?: VoiceMode.UNSPECIFIED,
            firstSpeaker = json["firstSpeaker"]?.takeIf { !it.isNull }?.let { parseFirstSpeaker(it) },
            enableDenoiser = json["enableDenoiser"]?.asBoolean() ?: false,
            enablePrefetch = json["enablePrefetch"]?.asBoolean() ?: false,
            enablePersonIdentity = json["enablePersonIdentity"]?.asBoolean() ?: false,
            enableWhisper = json["enableWhisper"]?.asBoolean() ?: false,
            enableEmotion = json["enableEmotion"]?.asBoolean() ?: false
        )
    }

    private fun parseAudioSettings(json: JsonNode): AudioSettings {
        return AudioSettings(
            input = json["input"]?.takeIf { !it.isNull }?.let { parseAudioInputSettings(it) },
            output = json["output"]?.takeIf { !it.isNull }?.let { parseAudioOutputSettings(it) }
        )
    }

    private fun parseAudioInputSettings(json: JsonNode): AudioInputSettings {
        return AudioInputSettings(
            model = json["model"]?.takeIf { !it.isNull }?.asText(),
            audioEncoding = json["audioEncoding"]?.asText()?.let { AudioEncoding.valueOf(it) }
                ?: AudioEncoding.UNSPECIFIED,
            sampleRate = json["sampleRate"]?.takeIf { !it.isNull }?.asInt(),
            silencePhrases = json["silencePhrases"]?.map { it.asText() } ?: emptyList(),
            silencePhrasesTimeout = json["silencePhrasesTimeoutMs"]?.takeIf { !it.isNull }?.asLong()?.milliseconds,
            silenceTimeout = json["silenceTimeoutMs"]?.takeIf { !it.isNull }?.asLong()?.milliseconds,
            stopPhrases = json["stopPhrases"]?.map { it.asText() } ?: emptyList(),
            ignorePhrases = json["ignorePhrases"]?.map { it.asText() } ?: emptyList()
        )
    }

    private fun parseAudioOutputSettings(json: JsonNode): AudioOutputSettings {
        return AudioOutputSettings(
            voice = json["voice"]?.takeIf { !it.isNull }?.asText(),
            audioEncoding = json["audioEncoding"]?.asText()?.let { AudioEncoding.valueOf(it) }
                ?: AudioEncoding.UNSPECIFIED
        )
    }

    private fun parseGigaChatSettings(json: JsonNode): GigaChatSettings {
        return GigaChatSettings(
            model = json["model"]?.takeIf { !it.isNull }?.asText(),
            temperature = json["temperature"]?.takeIf { !it.isNull }?.floatValue(),
            topP = json["topP"]?.takeIf { !it.isNull }?.floatValue(),
            repetitionPenalty = json["repetitionPenalty"]?.takeIf { !it.isNull }?.floatValue(),
            updateInterval = json["updateInterval"]?.takeIf { !it.isNull }?.floatValue(),
            profanityCheck = json["profanityCheck"]?.takeIf { !it.isNull }?.asBoolean(),
            filtersSettings = json["filtersSettings"]?.properties()?.associate {
                it.key to parseFilterSettings(it.value)
            } ?: emptyMap(),
            functions = json["functions"]?.map { parseFunctionDefinition(it) } ?: emptyList(),
            functionRegistry = json["functionRegistry"]?.takeIf { !it.isNull }?.let { parseFunctionRegistry(it) }
        )
    }

    private fun parseFilterSettings(json: JsonNode): FilterSettings {
        return FilterSettings(
            requestContent = json["requestContent"]?.takeIf { !it.isNull }?.let { parseRequestContentSettings(it) },
            responseContent = json["responseContent"]?.takeIf { !it.isNull }?.let { parseResponseContentSettings(it) }
        )
    }

    private fun parseRequestContentSettings(json: JsonNode): RequestContentSettings {
        return RequestContentSettings(
            neuro = json["neuro"]?.takeIf { !it.isNull }?.asBoolean(),
            blacklist = json["blacklist"]?.takeIf { !it.isNull }?.asBoolean(),
            whitelist = json["whitelist"]?.takeIf { !it.isNull }?.asBoolean()
        )
    }

    private fun parseResponseContentSettings(json: JsonNode): ResponseContentSettings {
        return ResponseContentSettings(
            blacklist = json["blacklist"]?.takeIf { !it.isNull }?.asBoolean()
        )
    }

    private fun parseFunctionDefinition(json: JsonNode): FunctionDefinition {
        return FunctionDefinition(
            name = json["name"].asText(),
            description = json["description"]?.takeIf { !it.isNull }?.asText(),
            parameters = json["parameters"]?.takeIf { !it.isNull }?.asText(),
            fewShotExamples = json["fewShotExamples"]?.map { parseFunctionExample(it) } ?: emptyList(),
            returnParameters = json["returnParameters"]?.takeIf { !it.isNull }?.asText()
        )
    }

    private fun parseFunctionExample(json: JsonNode): FunctionExample {
        return FunctionExample(
            request = json["request"].asText(),
            params = json["params"]?.map { param ->
                param["key"].asText() to param["value"].asText()
            } ?: emptyList()
        )
    }

    private fun parseFunctionRegistry(json: JsonNode): FunctionRegistry {
        return FunctionRegistry(
            profile = json["profile"]?.takeIf { !it.isNull }?.asText(),
            labels = json["labels"]?.map { it.asText() } ?: emptyList(),
            abFlags = json["abFlags"]?.takeIf { !it.isNull }?.asText()
        )
    }

    private fun parseInitialContext(json: JsonNode): InitialContext {
        return InitialContext(
            messages = json["messages"]?.map { parseMessage(it) } ?: emptyList()
        )
    }

    private fun parseMessage(json: JsonNode): Message {
        return Message(
            role = json["role"].asText(),
            content = json["content"].asText(),
            functionCall = json["functionCall"]?.takeIf { !it.isNull }?.let { parseFunctionCall(it) },
            functionName = json["functionName"]?.takeIf { !it.isNull }?.asText(),
            functionsStateId = json["functionsStateId"]?.takeIf { !it.isNull }?.asText(),
            attachments = json["attachments"]?.map { it.asText() } ?: emptyList()
        )
    }

    private fun parseFirstSpeaker(json: JsonNode): FirstSpeaker {
        return FirstSpeaker(
            type = json["type"]?.takeIf { !it.isNull }?.asText(),
            lockFirstIn = json["lockFirstIn"]?.takeIf { !it.isNull }?.asBoolean()
        )
    }
}
