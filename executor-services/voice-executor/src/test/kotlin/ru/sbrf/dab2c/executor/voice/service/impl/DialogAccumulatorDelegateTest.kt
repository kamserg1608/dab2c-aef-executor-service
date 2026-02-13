package ru.sbrf.dab2c.executor.voice.service.impl

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.domain.voice.AudioContent
import ru.sbrf.dab2c.executor.domain.voice.AudioSettings
import ru.sbrf.dab2c.executor.domain.voice.ErrorData
import ru.sbrf.dab2c.executor.domain.voice.InputTranscriptionData
import ru.sbrf.dab2c.executor.domain.voice.OutputTranscriptionData
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.domain.voice.WarningData
import ru.sbrf.dab2c.executor.voice.audit.ExternalInteractionAuditor
import ru.sbrf.dab2c.executor.voice.audit.ExternalInteractionRequest
import ru.sbrf.dab2c.executor.voice.grpc.context.MetadataElement
import ru.sbrf.dab2c.executor.voice.model.RequestMetadata
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.service.api.DialogTurnPublisher

class DialogAccumulatorDelegateTest {

    private lateinit var delegate: ChunkProcessingService
    private lateinit var dialogTurnPublisher: DialogTurnPublisher
    private lateinit var accumulator: DialogAccumulatorDelegate
    private lateinit var auditor: ExternalInteractionAuditor
    private lateinit var metadataContext: MetadataElement

    @BeforeEach
    fun setUp() {
        delegate = mockk()
        dialogTurnPublisher = mockk()
        auditor = mockk(relaxed = true)

        every { delegate.processRequestChunks(any()) } answers { firstArg() }
        every { delegate.processResponseChunks(any()) } answers { firstArg() }
        coEvery { dialogTurnPublisher.publishDialogTurn(any(), any(), any()) } returns Unit

        accumulator = DialogAccumulatorDelegate(delegate, dialogTurnPublisher, auditor)
        metadataContext = createMetadataContext()
    }

    @Test
    fun `processRequestChunks should pass through unchanged`() = runTest {
        withContext(metadataContext) {
            val requests = flowOf(
                VoiceRequest.Settings(createVoiceSettings()),
                VoiceRequest.Audio(AudioContent(audioChunk = byteArrayOf(1, 2, 3)))
            )

            val result = accumulator.processRequestChunks(requests).toList()

            assertEquals(2, result.size)
        }
    }

    @Test
    fun `processResponseChunks should pass through all response types`() = runTest {
        withContext(metadataContext) {
            val responses = flowOf(
                createInputTranscription("hello"),
                createWarning("test warning"),
                createOutputTranscription("hi there"),
                createError(status = 499, message = "Cancellation received from client")
            )

            val result = accumulator.processResponseChunks(responses).toList()

            assertEquals(4, result.size)
        }
    }

    @Test
    fun `should accumulate multiple input transcription chunks`() = runTest {
        withContext(metadataContext) {
            val responses = flowOf(
                createInputTranscription("Hel"),
                createInputTranscription("lo "),
                createInputTranscription("world")
            )

            val result = accumulator.processResponseChunks(responses).toList()

            assertEquals(3, result.size)
        }
    }

    @Test
    fun `should accumulate multiple output transcription chunks`() = runTest {
        withContext(metadataContext) {
            val responses = flowOf(
                createInputTranscription("question"),
                createOutputTranscription("ans"),
                createOutputTranscription("wer")
            )

            val result = accumulator.processResponseChunks(responses).toList()

            assertEquals(3, result.size)
        }
    }

    @Test
    fun `should complete dialog turn when input arrives after output`() = runTest {
        withContext(metadataContext) {
            val responses = flowOf(
                createInputTranscription("Hello"),
                createOutputTranscription("Hi"),
                createInputTranscription("How are you")
            )

            val result = accumulator.processResponseChunks(responses).toList()

            assertEquals(3, result.size)
        }
    }

    @Test
    fun `should handle multiple dialog turns`() = runTest {
        withContext(metadataContext) {
            val responses = flowOf(
                createInputTranscription("First question"),
                createOutputTranscription("First answer"),
                createWarning("Cancellation received from client"),
                createInputTranscription("Second question"),
                createOutputTranscription("Second answer"),
                createError(status = 499, message = "Cancellation received from client"),
                createInputTranscription("Third question")
            )

            accumulator.processResponseChunks(responses).toList()

            val successRequestSlot = slot<ExternalInteractionRequest>()

            coVerify(exactly = 1) {
                auditor.success(capture(successRequestSlot), any())
            }

            val rq = requireNotNull(successRequestSlot.captured.rqMessage)

            assertThat(rq).contains("First")
            assertThat(rq).contains("Second")
            assertThat(rq).contains("Cancellation")
        }
    }

    @Test
    fun `should handle chunked transcriptions in dialog turn`() = runTest {
        withContext(metadataContext) {
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
    }

    @Test
    fun `should handle empty transcription text`() = runTest {
        withContext(metadataContext) {
            val responses = flowOf(
                createInputTranscription(""),
                createInputTranscription("hello"),
                createOutputTranscription("")
            )

            val result = accumulator.processResponseChunks(responses).toList()

            assertEquals(3, result.size)
        }
    }

    @Test
    fun `should handle only input transcriptions without output`() = runTest {
        withContext(metadataContext) {
            val responses = flowOf(
                createInputTranscription("input1"),
                createInputTranscription("input2"),
                createInputTranscription("input3")
            )

            val result = accumulator.processResponseChunks(responses).toList()

            assertEquals(3, result.size)
        }
    }

    @Test
    fun `should handle mixed response types between transcriptions`() = runTest {
        withContext(metadataContext) {
            val responses = flowOf(
                createInputTranscription("hello"),
                createWarning("warning1"),
                createOutputTranscription("hi"),
                createWarning("warning2"),
                createInputTranscription("next")
            )

            val result = accumulator.processResponseChunks(responses).toList()

            assertEquals(5, result.size)
        }
    }

    @Nested
    inner class DialogPublishingTest {

        @Test
        fun `should publish dialog when dialog turn completes`() = runTest {
            withContext(metadataContext) {
                val responses = flowOf(
                    createInputTranscription("Hello"),
                    createOutputTranscription("Hi there"),
                    createInputTranscription("Next question")
                )

                accumulator.processResponseChunks(responses).toList()

                coVerify(exactly = 1) { dialogTurnPublisher.publishDialogTurn(any(), any(), any()) }
            }
        }

        @Test
        fun `should publish dialog with accumulated text`() = runTest {
            withContext(metadataContext) {
                val inputSlot = slot<String>()
                val outputSlot = slot<String>()
                val responseTimeSlot = slot<Long>()
                coEvery {
                    dialogTurnPublisher.publishDialogTurn(
                        capture(inputSlot),
                        capture(outputSlot),
                        capture(responseTimeSlot)
                    )
                } returns Unit

                val responses = flowOf(
                    createInputTranscription("How "),
                    createInputTranscription("are you?"),
                    createOutputTranscription("I am "),
                    createOutputTranscription("fine!"),
                    createInputTranscription("Great")
                )

                accumulator.processResponseChunks(responses).toList()

                assertEquals("How are you?", inputSlot.captured)
                assertEquals("I am fine!", outputSlot.captured)
            }
        }

        @Test
        fun `should not publish when no output before next input`() = runTest {
            withContext(metadataContext) {
                val responses = flowOf(
                    createInputTranscription("First"),
                    createInputTranscription("Second"),
                    createInputTranscription("Third")
                )

                accumulator.processResponseChunks(responses).toList()

                coVerify(exactly = 0) { dialogTurnPublisher.publishDialogTurn(any(), any(), any()) }
            }
        }

        @Test
        fun `should publish multiple dialogs for multiple turns`() = runTest {
            withContext(metadataContext) {
                val responses = flowOf(
                    createInputTranscription("First"),
                    createOutputTranscription("Answer 1"),
                    createInputTranscription("Second"),
                    createOutputTranscription("Answer 2"),
                    createInputTranscription("Third")
                )

                accumulator.processResponseChunks(responses).toList()

                coVerify(exactly = 2) { dialogTurnPublisher.publishDialogTurn(any(), any(), any()) }
            }
        }

        @Test
        fun `should calculate assistant response time from output generation`() = runTest {
            withContext(metadataContext) {
                val responseTimeSlot = slot<Long>()
                coEvery {
                    dialogTurnPublisher.publishDialogTurn(any(), any(), capture(responseTimeSlot))
                } returns Unit

                val responses = flowOf(
                    createInputTranscription("Hello"),
                    createOutputTranscription("Hi"),
                    createInputTranscription("Next")
                )

                accumulator.processResponseChunks(responses).toList()

                assertThat(responseTimeSlot.captured).isGreaterThanOrEqualTo(0L)
            }
        }

        @Test
        fun `should calculate assistant response time correctly for multiple turns`() = runTest {
            withContext(metadataContext) {
                val responseTimes = mutableListOf<Long>()
                coEvery {
                    dialogTurnPublisher.publishDialogTurn(any(), any(), capture(responseTimes))
                } returns Unit

                val responses = flowOf(
                    createInputTranscription("First"),
                    createOutputTranscription("Answer 1"),
                    createInputTranscription("Second"),
                    createOutputTranscription("Answer 2"),
                    createInputTranscription("Third")
                )

                accumulator.processResponseChunks(responses).toList()

                assertThat(responseTimes).hasSize(2)
                responseTimes.forEachIndexed { index, responseTime ->
                    assertThat(responseTime)
                        .describedAs("Assistant response time for turn ${index + 1}")
                        .isGreaterThanOrEqualTo(0L)
                        .isLessThan(10_000L)
                }
            }
        }
    }

    private fun createMetadataContext(): MetadataElement {
        val metadata = RequestMetadata(
            mapOf(
                X_TOKEN to "token-456",
                X_SESSION to "session-123",
                UFS_SESSION to "session-123",
                UFS_TOKEN to "token-456"
            )
        )
        return MetadataElement(metadata)
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

    private fun createWarning(message: String): VoiceResponse.Warning =
        VoiceResponse.Warning(
            WarningData(message = message)
        )

    private fun createError(status: Int, message: String): VoiceResponse.Error =
        VoiceResponse.Error(
            ErrorData(
                status = status,
                message = message
            )
        )

    private fun createVoiceSettings(): VoiceSettings = VoiceSettings(
        voiceCallId = "call-123",
        audio = AudioSettings()
    )

    private companion object {
        private const val X_TOKEN = "x-token"
        private const val X_SESSION = "x-session"
        private const val UFS_SESSION = "UFS-SESSION"
        private const val UFS_TOKEN = "UFS-TOKEN"
    }
}
