package ru.sbrf.dab2c.executor.voice.service.impl

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.domain.voice.AudioContent
import ru.sbrf.dab2c.executor.domain.voice.AudioSettings
import ru.sbrf.dab2c.executor.domain.voice.InputTranscriptionData
import ru.sbrf.dab2c.executor.domain.voice.OutputTranscriptionData
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.domain.voice.WarningData
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService

class DialogAccumulatorDelegateTest {

    private lateinit var delegate: ChunkProcessingService
    private lateinit var accumulator: DialogAccumulatorDelegate

    @BeforeEach
    fun setUp() {
        delegate = mockk()
        every { delegate.processRequestChunks(any()) } answers { firstArg() }
        every { delegate.processResponseChunks(any()) } answers { firstArg() }
        accumulator = DialogAccumulatorDelegate(delegate)
    }

    @Test
    fun `processRequestChunks should pass through unchanged`() = runTest {
        val requests = flowOf(
            VoiceRequest.Settings(createVoiceSettings()),
            VoiceRequest.Audio(AudioContent(audioChunk = byteArrayOf(1, 2, 3)))
        )

        val result = accumulator.processRequestChunks(requests).toList()

        assertEquals(2, result.size)
    }

    @Test
    fun `processResponseChunks should pass through all response types`() = runTest {
        val responses = flowOf(
            createInputTranscription("hello"),
            VoiceResponse.Warning(WarningData("test warning")),
            createOutputTranscription("hi there")
        )

        val result = accumulator.processResponseChunks(responses).toList()

        assertEquals(3, result.size)
    }

    @Test
    fun `should accumulate multiple input transcription chunks`() = runTest {
        val responses = flowOf(
            createInputTranscription("Hel"),
            createInputTranscription("lo "),
            createInputTranscription("world")
        )

        val result = accumulator.processResponseChunks(responses).toList()

        assertEquals(3, result.size)
    }

    @Test
    fun `should accumulate multiple output transcription chunks`() = runTest {
        val responses = flowOf(
            createInputTranscription("question"),
            createOutputTranscription("ans"),
            createOutputTranscription("wer")
        )

        val result = accumulator.processResponseChunks(responses).toList()

        assertEquals(3, result.size)
    }

    @Test
    fun `should complete dialog turn when input arrives after output`() = runTest {
        val responses = flowOf(
            createInputTranscription("Hello"),
            createOutputTranscription("Hi"),
            createInputTranscription("How are you")
        )

        val result = accumulator.processResponseChunks(responses).toList()

        assertEquals(3, result.size)
    }

    @Test
    fun `should handle multiple dialog turns`() = runTest {
        val responses = flowOf(
            createInputTranscription("First question"),
            createOutputTranscription("First answer"),
            createInputTranscription("Second question"),
            createOutputTranscription("Second answer"),
            createInputTranscription("Third question")
        )

        val result = accumulator.processResponseChunks(responses).toList()

        assertEquals(5, result.size)
    }

    @Test
    fun `should handle chunked transcriptions in dialog turn`() = runTest {
        val responses = flowOf(
            createInputTranscription("How "),
            createInputTranscription("are "),
            createInputTranscription("you?"),
            createOutputTranscription("I am "),
            createOutputTranscription("fine!"),
            createInputTranscription("Great")
        )

        val result = accumulator.processResponseChunks(responses).toList()

        assertEquals(6, result.size)
    }

    @Test
    fun `should handle empty transcription text`() = runTest {
        val responses = flowOf(
            createInputTranscription(""),
            createInputTranscription("hello"),
            createOutputTranscription("")
        )

        val result = accumulator.processResponseChunks(responses).toList()

        assertEquals(3, result.size)
    }

    @Test
    fun `should handle only input transcriptions without output`() = runTest {
        val responses = flowOf(
            createInputTranscription("input1"),
            createInputTranscription("input2"),
            createInputTranscription("input3")
        )

        val result = accumulator.processResponseChunks(responses).toList()

        assertEquals(3, result.size)
    }

    @Test
    fun `should handle mixed response types between transcriptions`() = runTest {
        val responses = flowOf(
            createInputTranscription("hello"),
            VoiceResponse.Warning(WarningData("warning1")),
            createOutputTranscription("hi"),
            VoiceResponse.Warning(WarningData("warning2")),
            createInputTranscription("next")
        )

        val result = accumulator.processResponseChunks(responses).toList()

        assertEquals(5, result.size)
    }

    private fun createInputTranscription(text: String): VoiceResponse.InputTranscription =
        VoiceResponse.InputTranscription(
            InputTranscriptionData(
                text = text,
                timestamp = System.currentTimeMillis()
            )
        )

    private fun createOutputTranscription(text: String): VoiceResponse.OutputTranscription =
        VoiceResponse.OutputTranscription(
            OutputTranscriptionData(
                text = text,
                functionsStateId = "state-1",
                finishReason = "stop",
                timestamp = System.currentTimeMillis()
            )
        )

    private fun createVoiceSettings(): VoiceSettings = VoiceSettings(
        voiceCallId = "call-123",
        audio = AudioSettings()
    )
}
