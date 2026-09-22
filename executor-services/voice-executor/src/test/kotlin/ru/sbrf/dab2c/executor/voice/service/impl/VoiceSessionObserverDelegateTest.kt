package ru.sbrf.dab2c.executor.voice.service.impl

import com.google.protobuf.ByteString
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audioContent
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.contentFromClient
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.contentFromModel
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.inputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.outputTranscription
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.warning
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.session.observer.ErrorEmitted
import ru.sbrf.dab2c.executor.voice.session.observer.FunctionCallReceived
import ru.sbrf.dab2c.executor.voice.session.observer.LlmUsage
import ru.sbrf.dab2c.executor.voice.session.observer.TurnCompleted
import ru.sbrf.dab2c.executor.voice.session.observer.VoiceSessionObserver
import ru.sbrf.dab2c.executor.voice.session.observer.WarningEmitted
import ru.sbrf.dab2c.executor.voice.test.IncrementingTimeProvider
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.error as protoError
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCall as protoFunctionCallDsl

class VoiceSessionObserverDelegateTest {

    private lateinit var delegate: ChunkProcessingService
    private lateinit var observer: VoiceSessionObserver
    private lateinit var accumulator: VoiceSessionObserverDelegate

    @BeforeEach
    fun setUp() {
        delegate = mockk()
        observer = mockk(relaxed = true)

        every { delegate.processRequestChunks(any()) } answers { firstArg() }
        every { delegate.processResponseChunks(any()) } answers { firstArg() }

        accumulator = VoiceSessionObserverDelegate(delegate, observer, IncrementingTimeProvider())
    }

    @Test
    fun `processRequestChunks passes through unchanged`() = runTest {
        val requests = flowOf(
            gigaVoiceRequest {
                input = contentFromClient {
                    audioContent = audioContent { audioChunk = ByteString.copyFrom(byteArrayOf(1, 2, 3)) }
                }
            }
        )

        val result = accumulator.processRequestChunks(requests).toList()

        assertEquals(1, result.size)
    }

    @Test
    fun `processResponseChunks passes through all response types`() = runTest {
        val responses = flowOf(
            createInputTranscription("hello"),
            createWarning("test warning"),
            createOutputTranscription("hi there"),
            createError(status = 499, message = "Cancellation received from client")
        )

        val result = accumulator.processResponseChunks(responses).toList()

        assertEquals(4, result.size)
    }

    @Test
    fun `should publish one TurnCompleted per dialog turn`() = runTest {
        val responses = flowOf(
            createInputTranscription("First"),
            createOutputTranscription("Answer 1"),
            createInputTranscription("Second"),
            createOutputTranscription("Answer 2"),
            createInputTranscription("Third"),
        )

        accumulator.processResponseChunks(responses).toList()

        coVerify(exactly = 3) { observer.onTurnCompleted(any()) }
    }

    @Test
    fun `joins chunked transcriptions inside a single turn`() = runTest {
        val captured = mutableListOf<TurnCompleted>()
        coEvery { observer.onTurnCompleted(capture(captured)) } just Runs

        val responses = flowOf(
            createInputTranscription("How "),
            createInputTranscription("are "),
            createInputTranscription("you?"),
            createOutputTranscription("I am "),
            createOutputTranscription("fine!"),
            createInputTranscription("Great"),
        )

        accumulator.processResponseChunks(responses).toList()

        assertEquals(2, captured.size)
        assertEquals("How are you?", captured[0].userReplica?.text)
        assertEquals("I am fine!", captured[0].assistantReplica?.text)
        assertEquals("Great", captured[1].userReplica?.text)
        assertThat(captured[1].assistantReplica).isNull()
    }

    @Test
    fun `flushes trailing input-only turn on session completion`() = runTest {
        val captured = mutableListOf<TurnCompleted>()
        coEvery { observer.onTurnCompleted(capture(captured)) } just Runs

        val responses = flowOf(
            createInputTranscription("First"),
            createInputTranscription("Second"),
        )

        accumulator.processResponseChunks(responses).toList()

        assertEquals(1, captured.size)
        assertEquals("FirstSecond", captured[0].userReplica?.text)
        assertThat(captured[0].assistantReplica).isNull()
    }

    @Test
    fun `flushes trailing output-only turn on session completion`() = runTest {
        val captured = mutableListOf<TurnCompleted>()
        coEvery { observer.onTurnCompleted(capture(captured)) } just Runs

        val responses = flowOf(createOutputTranscription("World"))

        accumulator.processResponseChunks(responses).toList()

        assertEquals(1, captured.size)
        assertThat(captured[0].userReplica).isNull()
        assertEquals("World", captured[0].assistantReplica?.text)
    }

    @Test
    fun `captures TurnEvent items for warning and error chunks within a turn`() = runTest {
        val captured = mutableListOf<TurnCompleted>()
        coEvery { observer.onTurnCompleted(capture(captured)) } just Runs

        val responses = flowOf(
            createInputTranscription("Hello"),
            createWarning("low confidence"),
            createError(400, "Bad"),
            createOutputTranscription("Sorry"),
            createInputTranscription("Next"),
        )

        accumulator.processResponseChunks(responses).toList()

        val events = captured[0].turnEvents
        assertEquals(2, events.size)
        assertEquals("low confidence", (events.single { it is WarningEmitted } as WarningEmitted).message)
        val err = events.single { it is ErrorEmitted } as ErrorEmitted
        assertEquals(400, err.status)
        assertEquals("Bad", err.message)
    }

    @Test
    fun `captures function_call as TurnEvent`() = runTest {
        val captured = mutableListOf<TurnCompleted>()
        coEvery { observer.onTurnCompleted(capture(captured)) } just Runs

        val responses = flowOf(
            createInputTranscription("Check"),
            gigaVoiceResponse {
                functionCall = functionCalling {
                    functionCall = protoFunctionCallDsl {
                        name = "get_balance"
                        arguments = """{"id":"1"}"""
                    }
                    timestamp = 999L
                }
            },
            createOutputTranscription("Balance is 1000"),
            createInputTranscription("Thanks"),
        )

        accumulator.processResponseChunks(responses).toList()

        val fc = captured[0].turnEvents.single { it is FunctionCallReceived } as FunctionCallReceived
        assertEquals("get_balance", fc.name)
        assertEquals("""{"id":"1"}""", fc.arguments)
    }

    @Test
    fun `accumulates totalTokens from additionalData usage chunks`() = runTest {
        val captured = mutableListOf<TurnCompleted>()
        coEvery { observer.onTurnCompleted(capture(captured)) } just Runs

        val responses = flowOf(
            createInputTranscription("Hello"),
            gigaVoiceResponse {
                output = contentFromModel {
                    additionalData = ru.sbrf.dab2c.executor.clients.gigavoice.proto.additionalData {
                        usage = ru.sbrf.dab2c.executor.clients.gigavoice.proto.usage { totalTokens = 42 }
                    }
                }
            },
            createOutputTranscription("Hi"),
            createInputTranscription("Next"),
        )

        accumulator.processResponseChunks(responses).toList()

        assertEquals(42, captured[0].totalTokens)
    }

    @Test
    fun `accumulates llm usage, model and finish reason within a turn`() = runTest {
        val captured = mutableListOf<TurnCompleted>()
        coEvery { observer.onTurnCompleted(capture(captured)) } just Runs

        val responses = flowOf(
            createInputTranscription("Hello"),
            createUsage(prompt = 40, completion = 10, total = 50, precached = 1, reason = "function_call"),
            createOutputTranscription("Hi"),
            createUsage(prompt = 24, completion = 14, total = 38, precached = 0, reason = "stop"),
            createInputTranscription("Next"),
        )

        accumulator.processResponseChunks(responses).toList()

        assertEquals(
            LlmUsage(
                promptTokens = 64,
                completionTokens = 24,
                totalTokens = 88,
                precachedPromptTokens = 1,
                model = "GigaChat:1.0.26.20",
                finishReason = "stop",
            ),
            captured[0].llmUsage
        )
        assertEquals(88, captured[0].totalTokens)
    }

    private fun createUsage(prompt: Int, completion: Int, total: Int, precached: Int, reason: String) =
        gigaVoiceResponse {
            output = contentFromModel {
                additionalData = ru.sbrf.dab2c.executor.clients.gigavoice.proto.additionalData {
                    usage = ru.sbrf.dab2c.executor.clients.gigavoice.proto.usage {
                        promptTokens = prompt
                        completionTokens = completion
                        totalTokens = total
                        precachedPromptTokens = precached
                    }
                    gigachatModelInfo = ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaChatModelInfo {
                        name = "GigaChat"
                        version = "1.0.26.20"
                    }
                    finishReason = reason
                }
            }
        }

    @Test
    fun `resets per-turn state between turns`() = runTest {
        val captured = mutableListOf<TurnCompleted>()
        coEvery { observer.onTurnCompleted(capture(captured)) } just Runs

        val responses = flowOf(
            createInputTranscription("First"),
            createWarning("warn1"),
            createOutputTranscription("Answer 1"),
            createInputTranscription("Second"),
            createOutputTranscription("Answer 2"),
            createInputTranscription("Third"),
        )

        accumulator.processResponseChunks(responses).toList()

        assertEquals(3, captured.size)
        assertEquals(1, captured[0].turnEvents.size)
        assertEquals(0, captured[1].turnEvents.size)
        assertEquals(0, captured[2].turnEvents.size)
    }

    @Nested
    inner class SessionCompletionTest {

        @Test
        fun `emits onSessionCompleted with null cause on normal completion`() = runTest {
            val responses = flowOf(
                createInputTranscription("Hello"),
                createOutputTranscription("World"),
            )

            accumulator.processResponseChunks(responses).toList()

            coVerify(exactly = 1) { observer.onSessionCompleted(null) }
        }

        @Test
        fun `emits onSessionCompleted with cause on flow failure`() = runTest {
            val responses = flow<GigaVoiceResponse> {
                emit(createInputTranscription("Hello"))
                throw RuntimeException("boom")
            }

            runCatching { accumulator.processResponseChunks(responses).toList() }

            val slot = slot<Throwable>()
            coVerify(exactly = 1) { observer.onSessionCompleted(capture(slot)) }
            assertThat(slot.captured).isInstanceOf(RuntimeException::class.java)
            assertEquals("boom", slot.captured.message)
        }

        @Test
        fun `propagates cause without tolerant filtering — auditor itself decides`() = runTest {
            val responses = flow<GigaVoiceResponse> {
                emit(createInputTranscription("Hello"))
                throw CancellationException("client cancelled")
            }

            runCatching { accumulator.processResponseChunks(responses).toList() }

            val slot = slot<Throwable>()
            coVerify(exactly = 1) { observer.onSessionCompleted(capture(slot)) }
            assertThat(slot.captured).isInstanceOf(CancellationException::class.java)
        }

        @Test
        fun `emits onSessionCompleted when pending turn flush throws CancellationException`() = runTest {
            val flushFailure = CancellationException("observer cancelled")
            coEvery { observer.onTurnCompleted(any()) } throws flushFailure
            val responses = flowOf(
                createInputTranscription("Hello"),
                createOutputTranscription("World"),
            )

            val thrown = runCatching { accumulator.processResponseChunks(responses).toList() }.exceptionOrNull()

            assertThat(thrown).isSameAs(flushFailure)
            coVerify(exactly = 1) { observer.onSessionCompleted(null) }
        }
    }

    private fun createInputTranscription(text: String): GigaVoiceResponse = gigaVoiceResponse {
        inputTranscription = inputTranscription {
            this.text = text
            timestamp = System.currentTimeMillis()
        }
    }

    private fun createOutputTranscription(text: String): GigaVoiceResponse = gigaVoiceResponse {
        outputTranscription = outputTranscription {
            this.text = text
            functionsStateId = "state-1"
            finishReason = "stop"
            timestamp = System.currentTimeMillis()
        }
    }

    private fun createWarning(message: String): GigaVoiceResponse = gigaVoiceResponse {
        warning = warning { this.message = message }
    }

    private fun createError(status: Int, message: String): GigaVoiceResponse = gigaVoiceResponse {
        error = protoError {
            this.status = status
            this.message = message
        }
    }
}
