package ru.sbrf.dab2c.executor.clients.gigavoice.mapper

import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Context
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCall
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.context
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCall
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionResult
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.FunctionResultData
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.domain.voice.FunctionCall as DomainFunctionCall

/** Converts proto Settings to domain VoiceSettings. */
fun Settings.toDomain(): VoiceSettings = IvrDomainMapper.toDomainSettings(this)

/** Converts domain VoiceSettings to proto Settings. */
fun VoiceSettings.toProto(): Settings = GigaVoiceDomainMapper.toProtoSettings(this)

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
