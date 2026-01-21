package ru.sbrf.dab2c.executor.it.support.fixtures

import GigaVoiceProtocol.GigaVoice.GigaVoiceResponse
import GigaVoiceProtocol.audio
import GigaVoiceProtocol.contentFromModel
import GigaVoiceProtocol.functionCall
import GigaVoiceProtocol.functionCalling
import GigaVoiceProtocol.gigaVoiceResponse
import GigaVoiceProtocol.inputTranscription
import GigaVoiceProtocol.outputTranscription
import com.google.protobuf.ByteString

object GigaVoiceResponseFixtures {

    fun inputTranscriptionResponse(
        text: String = "user input",
        timestamp: Long = System.currentTimeMillis(),
    ): GigaVoiceResponse = gigaVoiceResponse {
        inputTranscription = inputTranscription {
            this.text = text
            this.timestamp = timestamp
        }
    }

    fun outputTranscriptionResponse(
        text: String = "response",
    ): GigaVoiceResponse = gigaVoiceResponse {
        outputTranscription = outputTranscription { this.text = text }
    }

    fun functionCallingResponse(
        name: String,
        arguments: String,
        timestamp: Long = System.currentTimeMillis(),
    ): GigaVoiceResponse = gigaVoiceResponse {
        functionCall = functionCalling {
            functionCall = functionCall {
                this.name = name
                this.arguments = arguments
            }
            this.timestamp = timestamp
        }
    }

    fun audioResponse(
        chunkId: Int,
    ): GigaVoiceResponse = gigaVoiceResponse {
        output = contentFromModel {
            audio = audio {
                audioChunk = ByteString.copyFrom(byteArrayOf(chunkId.toByte()))
            }
        }
    }
}
