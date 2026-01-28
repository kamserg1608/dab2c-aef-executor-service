package ru.sbrf.dab2c.executor.it.support.fixtures

import com.google.protobuf.ByteString
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audio
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.contentFromModel
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCall
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.inputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.outputTranscription

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
