package ru.sbrf.dab2c.executor.library.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.module.SimpleModule
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat

/**
 * Jackson module that round-trips any [com.google.protobuf.Message] via the canonical
 * proto3 JSON mapping (Google's `JsonFormat`). Read-side handles every concrete `Message`
 * subtype Jackson is asked to deserialize; write-side covers every `Message` instance via
 * inheritance lookup.
 */
class ProtobufModule : SimpleModule(NAME) {

    init {
        addSerializer(Message::class.java, ProtobufMessageSerializer)
        setDeserializerModifier(ProtobufDeserializerModifier)
    }

    private companion object {
        const val NAME: String = "ProtobufModule"
    }
}

private object ProtobufMessageSerializer : JsonSerializer<Message>() {

    private val printer: JsonFormat.Printer = JsonFormat.printer()
        .omittingInsignificantWhitespace()
        .alwaysPrintFieldsWithNoPresence()

    override fun serialize(value: Message, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeRawValue(printer.print(value))
    }
}

private object ProtobufDeserializerModifier : BeanDeserializerModifier() {

    override fun modifyDeserializer(
        config: DeserializationConfig,
        beanDesc: BeanDescription,
        deserializer: JsonDeserializer<*>
    ): JsonDeserializer<*> {
        val cls = beanDesc.beanClass
        return if (Message::class.java.isAssignableFrom(cls)) {
            @Suppress("UNCHECKED_CAST")
            ProtobufMessageDeserializer(cls as Class<out Message>)
        } else {
            deserializer
        }
    }
}

private class ProtobufMessageDeserializer(
    private val messageClass: Class<out Message>
) : JsonDeserializer<Message>() {

    private val parser: JsonFormat.Parser = JsonFormat.parser().ignoringUnknownFields()

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Message {
        val node: JsonNode = p.readValueAsTree()
        val builder = messageClass.getMethod("newBuilder").invoke(null) as Message.Builder
        parser.merge(node.toString(), builder)
        return builder.build()
    }
}
