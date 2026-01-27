package ru.sbrf.dab2c.executor.it.support.fixtures

import ru.sbrf.dab2c.executor.clients.ivr.proto.AudioContent
import ru.sbrf.dab2c.executor.clients.ivr.proto.AudioSettings
import ru.sbrf.dab2c.executor.clients.ivr.proto.ContentFromClient
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrRequest
import ru.sbrf.dab2c.executor.clients.ivr.proto.Settings
import ru.sbrf.dab2c.executor.clients.ivr.proto.audioContent
import ru.sbrf.dab2c.executor.clients.ivr.proto.audioSettings
import ru.sbrf.dab2c.executor.clients.ivr.proto.contentForSynthesis
import ru.sbrf.dab2c.executor.clients.ivr.proto.contentFromClient
import ru.sbrf.dab2c.executor.clients.ivr.proto.context
import ru.sbrf.dab2c.executor.clients.ivr.proto.functionResult
import ru.sbrf.dab2c.executor.clients.ivr.proto.ivrRequest
import ru.sbrf.dab2c.executor.clients.ivr.proto.settings

object IvrRequestFixtures {

    fun settingsRequest(
        voiceCallId: String = "test-call-id",
        audioSettings: AudioSettings = audioSettings {},
    ): IvrRequest = ivrRequest {
        settings = settings {
            this.voiceCallId = voiceCallId
            audio = audioSettings
        }
    }

    fun settingsRequest(
        voiceCallId: String = "test-call-id",
        block: Settings.Builder.() -> Unit,
    ): IvrRequest = ivrRequest {
        settings = Settings.newBuilder()
            .setVoiceCallId(voiceCallId)
            .setAudio(AudioSettings.getDefaultInstance())
            .apply(block)
            .build()
    }

    fun contextRequest(
        content: String = "{}",
    ): IvrRequest = ivrRequest {
        context = context { this.content = content }
    }

    fun audioRequest(
        speechStart: Boolean = false,
        speechEnd: Boolean = false,
    ): IvrRequest = ivrRequest {
        input = contentFromClient {
            audioContent = audioContent {
                this.speechStart = speechStart
                this.speechEnd = speechEnd
            }
        }
    }

    fun audioRequest(
        block: AudioContent.Builder.() -> Unit,
    ): IvrRequest = ivrRequest {
        input = contentFromClient {
            audioContent = AudioContent.newBuilder().apply(block).build()
        }
    }

    fun functionResultRequest(
        functionName: String,
        content: String,
    ): IvrRequest = ivrRequest {
        functionResult = functionResult {
            this.functionName = functionName
            this.content = content
        }
    }

    fun inputRequest(
        block: ContentFromClient.Builder.() -> Unit,
    ): IvrRequest = ivrRequest {
        input = ContentFromClient.newBuilder().apply(block).build()
    }

    fun textForSynthesisRequest(
        text: String,
        isFinal: Boolean = false,
    ): IvrRequest = ivrRequest {
        input = contentFromClient {
            contentForSynthesis = contentForSynthesis {
                this.text = text
                this.isFinal = isFinal
            }
        }
    }
}
