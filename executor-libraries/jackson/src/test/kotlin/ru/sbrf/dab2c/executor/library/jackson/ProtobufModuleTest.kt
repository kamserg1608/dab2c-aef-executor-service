package ru.sbrf.dab2c.executor.library.jackson

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.protobuf.duration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audioSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.context
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCall
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaChatSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.input
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.output
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.settings

/**
 * Verifies that the shared [ObjectMappers.MAPPER] round-trips arbitrary `com.google.protobuf.Message`
 * instances through the canonical proto3 JSON mapping, courtesy of [ProtobufModule].
 */
class ProtobufModuleTest {

    private val mapper = ObjectMappers.MAPPER

    @Nested
    inner class Serialize {

        @Test
        fun `snake_case proto fields are emitted as camelCase JSON`() {
            val proto = settings {
                voiceCallId = "call-123"
                audio = audioSettings { }
                disableVad = true
            }

            val json = mapper.readTree(mapper.writeValueAsString(proto))

            assertThat(json.has("voiceCallId")).isTrue()
            assertThat(json.get("voiceCallId").asText()).isEqualTo("call-123")
            assertThat(json.has("disableVad")).isTrue()
            assertThat(json.get("disableVad").asBoolean()).isTrue()
        }

        @Test
        fun `non-presence fields emit defaults thanks to alwaysPrintFieldsWithNoPresence`() {
            val proto = settings { audio = audioSettings { } }

            val json = mapper.readTree(mapper.writeValueAsString(proto))

            assertThat(json.has("voiceCallId")).isTrue()
            assertThat(json.get("voiceCallId").asText()).isEqualTo("")
            assertThat(json.has("flags")).isTrue()
            assertThat(json.get("flags").isArray).isTrue()
            assertThat(json.get("flags")).isEmpty()
        }

        @Test
        fun `optional fields with explicit presence are omitted when unset per proto3 JSON spec`() {
            val proto = settings {
                voiceCallId = "call-123"
                audio = audioSettings { }
            }

            val json = mapper.readTree(mapper.writeValueAsString(proto))

            assertThat(json.has("disableVad")).isFalse()
            assertThat(json.has("enableTranscribeInput")).isFalse()
            assertThat(json.has("mode")).isFalse()
        }

        @Test
        fun `nested messages serialize as nested JSON objects`() {
            val proto = settings {
                voiceCallId = "call-123"
                audio = audioSettings { output = output { voice = "alice" } }
            }

            val json = mapper.readTree(mapper.writeValueAsString(proto))

            assertThat(json.get("audio").isObject).isTrue()
            assertThat(json.get("audio").get("output").get("voice").asText()).isEqualTo("alice")
        }

        @Test
        fun `repeated fields serialize as JSON arrays`() {
            val proto = settings {
                voiceCallId = "call-123"
                audio = audioSettings { }
                flags.addAll(listOf("alpha", "beta", "gamma"))
            }

            val json = mapper.readTree(mapper.writeValueAsString(proto))

            assertThat(json.get("flags").isArray).isTrue()
            assertThat(json.get("flags").map { it.asText() })
                .containsExactly("alpha", "beta", "gamma")
        }

        @Test
        fun `proto3 enums serialize as their string name`() {
            val proto = settings {
                voiceCallId = "call-123"
                audio = audioSettings { }
                mode = Settings.Mode.GIGACHAT_SYNTHESIS
            }

            val json = mapper.readTree(mapper.writeValueAsString(proto))

            assertThat(json.get("mode").asText()).isEqualTo("GIGACHAT_SYNTHESIS")
        }

        @Test
        fun `Duration well-known type serializes to its canonical string form`() {
            val proto = input {
                silenceTimeout = duration { seconds = 30L }
            }

            val json = mapper.readTree(mapper.writeValueAsString(proto))

            assertThat(json.get("silenceTimeout").asText()).isEqualTo("30s")
        }

        @Test
        fun `a Map containing proto messages serializes them as structured JSON, not strings`() {
            val payload = mapOf(
                "endpoint" to "/settings",
                "voiceSettings" to settings {
                    voiceCallId = "call-123"
                    audio = audioSettings { }
                },
                "contextData" to context { content = """{"foo":"bar"}""" }
            )

            val json = mapper.readTree(mapper.writeValueAsString(payload))

            assertThat(json.get("endpoint").asText()).isEqualTo("/settings")
            assertThat(json.get("voiceSettings").isObject).isTrue()
            assertThat(json.get("voiceSettings").get("voiceCallId").asText()).isEqualTo("call-123")
            assertThat(json.get("contextData").isObject).isTrue()
            assertThat(json.get("contextData").get("content").asText()).isEqualTo("""{"foo":"bar"}""")
        }
    }

    @Nested
    inner class Deserialize {

        @Test
        fun `Jackson reflectively obtains newBuilder for any concrete Message subtype`() {
            val json = """{"voiceCallId":"call-123","disableVad":true,"flags":["x","y"]}"""

            val result: Settings = mapper.readValue(json)

            assertThat(result.voiceCallId).isEqualTo("call-123")
            assertThat(result.disableVad).isTrue()
            assertThat(result.flagsList).containsExactly("x", "y")
        }

        @Test
        fun `unknown fields are ignored on read`() {
            val json = """{"voiceCallId":"call-123","mysteryFutureField":42,"audio":{}}"""

            val result: Settings = mapper.readValue(json)

            assertThat(result.voiceCallId).isEqualTo("call-123")
        }

        @Test
        fun `enum strings deserialize back to their proto enum values`() {
            val json = """{"voiceCallId":"call-123","mode":"GIGACHAT"}"""

            val result: Settings = mapper.readValue(json)

            assertThat(result.mode).isEqualTo(Settings.Mode.GIGACHAT)
        }

        @Test
        fun `Duration canonical string deserializes to a proto Duration`() {
            val json = """{"silenceTimeout":"45s"}"""

            val result = mapper.readValue<ru.sbrf.dab2c.executor.clients.gigavoice.proto.Input>(json)

            assertThat(result.silenceTimeout.seconds).isEqualTo(45L)
        }
    }

    @Nested
    inner class RoundTrip {

        @Test
        fun `serialize-then-deserialize on a richly populated Settings yields an equal message`() {
            val original = settings {
                voiceCallId = "round-trip"
                audio = audioSettings {
                    input = input {
                        model = "asr-1"
                        silenceTimeout = duration { seconds = 30L }
                    }
                    output = output { voice = "alice" }
                }
                gigachat = gigaChatSettings { preset = "preset-a" }
                disableVad = true
                enableTranscribeInput = true
                flags.addAll(listOf("alpha", "beta"))
                mode = Settings.Mode.RECOGNIZE_GIGACHAT_SYNTHESIS
            }

            val json = mapper.writeValueAsString(original)
            val restored: Settings = mapper.readValue(json)

            assertThat(restored).isEqualTo(original)
        }

        @Test
        fun `roundtrip preserves FunctionCalling with nested FunctionCall and timestamp`() {
            val original = functionCalling {
                functionCall = functionCall {
                    name = "get_balance"
                    arguments = """{"id":"42"}"""
                }
                timestamp = 1_700_000_000L
            }

            val restored = mapper.readValue<ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling>(
                mapper.writeValueAsString(original)
            )

            assertThat(restored).isEqualTo(original)
        }

        @Test
        fun `roundtrip preserves FunctionResult with optional functionName`() {
            val original = functionResult {
                content = """{"balance":1000}"""
                functionName = "get_balance"
            }

            val restored = mapper.readValue<ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionResult>(
                mapper.writeValueAsString(original)
            )

            assertThat(restored).isEqualTo(original)
        }
    }
}
