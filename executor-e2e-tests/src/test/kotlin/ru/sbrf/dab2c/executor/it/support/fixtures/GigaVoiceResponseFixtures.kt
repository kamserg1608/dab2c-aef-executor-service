package ru.sbrf.dab2c.executor.it.support.fixtures

import com.google.protobuf.ByteString
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.additionalData
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audio
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.contentFromModel
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.error
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCall
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaChatModelInfo
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.inputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.outputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.platformFunctionProcessing
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.serviceInfo
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.serviceVersion
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.usage
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.warning

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

    fun finalAudioResponse(): GigaVoiceResponse = gigaVoiceResponse {
        output = contentFromModel {
            audio = audio {
                audioChunk = ByteString.copyFrom(byteArrayOf(0))
                isFinal = true
            }
        }
    }

    fun additionalDataResponse() = gigaVoiceResponse {
        output = contentFromModel {
            additionalData = additionalData {
                usage = usage {
                    promptTokens = 100
                    completionTokens = 200
                    totalTokens = 300
                }
                gigachatModelInfo = gigaChatModelInfo {
                    name = "test-model"
                    version = "1.2.0"
                }
                finishReason = "stop"
            }
        }
    }

    fun warningResponse(
        message: String = "test warning",
    ): GigaVoiceResponse = gigaVoiceResponse {
        warning = warning { this.message = message }
    }

    fun errorResponse(
        status: Int = 503,
        message: String = "test error",
    ): GigaVoiceResponse = gigaVoiceResponse {
        error = error {
            this.status = status
            this.message = message
        }
    }

    fun serviceInfo(
        name: String = "mock",
        versionName: String = "1",
        buildName: String = "b1"
    ): GigaVoiceResponse = gigaVoiceResponse {
        serviceInfo = serviceInfo {
            services.add(
                serviceVersion {
                    serviceName = name
                    version = versionName
                    build = buildName
                }
            )
        }
    }

    fun platformFunctionProcessing(
        functionName: String = "function",
    ): GigaVoiceResponse = gigaVoiceResponse {
        platformFunctionProcessing = platformFunctionProcessing {
            name = functionName
            timestamp = System.currentTimeMillis()
        }
    }
}
