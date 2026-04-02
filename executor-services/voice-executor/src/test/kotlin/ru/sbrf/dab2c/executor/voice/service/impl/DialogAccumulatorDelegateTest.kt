package ru.sbrf.dab2c.executor.voice.service.impl

import io.grpc.Status
import io.grpc.StatusException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.Parameter
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogTurnExtra
import ru.sbrf.dab2c.executor.clients.kap.producer.model.ErrorPayload
import ru.sbrf.dab2c.executor.clients.kap.producer.model.FunctionCallPayload
import ru.sbrf.dab2c.executor.clients.kap.producer.model.WarningPayload
import ru.sbrf.dab2c.executor.domain.voice.AudioContent
import ru.sbrf.dab2c.executor.domain.voice.AudioOutput
import ru.sbrf.dab2c.executor.domain.voice.AudioSettings
import ru.sbrf.dab2c.executor.domain.voice.ContentFromModel
import ru.sbrf.dab2c.executor.domain.voice.ErrorData
import ru.sbrf.dab2c.executor.domain.voice.FunctionCall
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.InputTranscriptionData
import ru.sbrf.dab2c.executor.domain.voice.OutputTranscriptionData
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.domain.voice.WarningData
import ru.sbrf.dab2c.executor.library.audit.model.InteractionAuditRequest
import ru.sbrf.dab2c.executor.library.audit.port.InteractionAuditor
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.HeadersElement
import ru.sbrf.dab2c.executor.voice.model.VoiceSessionFeatureToggles
import ru.sbrf.dab2c.executor.voice.model.VoiceSessionFeatureToggles.Companion.KAP_SEND_EXTRA
import ru.sbrf.dab2c.executor.voice.model.VoiceSessionFeatureTogglesElement
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.service.api.DialogTurnPublisher
import ru.sbrf.dab2c.executor.voice.test.IncrementingTimeProvider

class DialogAccumulatorDelegateTest {

    private lateinit var delegate: ChunkProcessingService
    private lateinit var dialogTurnPublisher: DialogTurnPublisher
    private lateinit var accumulator: DialogAccumulatorDelegate
    private lateinit var auditor: InteractionAuditor
    private lateinit var headersElement: HeadersElement
    private lateinit var togglesElement: VoiceSessionFeatureTogglesElement

    @BeforeEach
    fun setUp() {
        delegate = mockk()
        dialogTurnPublisher = mockk()
        auditor = mockk(relaxed = true)

        every { delegate.processRequestChunks(any()) } answers { firstArg() }
        every { delegate.processResponseChunks(any()) } answers { firstArg() }
        coEvery { dialogTurnPublisher.publishDialogTurn(any(), any(), any(), any(), any()) } returns Unit

        accumulator = DialogAccumulatorDelegate(delegate, dialogTurnPublisher, auditor, IncrementingTimeProvider())
        headersElement = HeadersElement(Headers(mapOf("x-token" to "token-456", "x-session" to "session-123")))
        togglesElement = togglesElement(kapSendExtra = false)
    }

    @Test
    fun `processRequestChunks should pass through unchanged`() = runTest {
        withContext(headersElement + togglesElement) {
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
        withContext(headersElement + togglesElement) {
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
        withContext(headersElement + togglesElement) {
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
        withContext(headersElement + togglesElement) {
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
        withContext(headersElement + togglesElement) {
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
        withContext(headersElement + togglesElement) {
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

            coVerify(exactly = 3) { auditor.success(any()) }
        }
    }

    @Test
    fun `should handle chunked transcriptions in dialog turn`() = runTest {
        withContext(headersElement + togglesElement) {
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
        withContext(headersElement + togglesElement) {
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
        withContext(headersElement + togglesElement) {
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
        withContext(headersElement + togglesElement) {
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
    inner class AuditOnCompletionTest {

        @Test
        fun `should flush trailing turn and send per-turn success audit on normal completion`() = runTest {
            withContext(headersElement + togglesElement) {
                val responses = flowOf(
                    createInputTranscription("Hello"),
                    createOutputTranscription("World")
                )

                accumulator.processResponseChunks(responses).toList()

                val requestSlot = slot<InteractionAuditRequest>()
                coVerify(exactly = 1) { auditor.success(capture(requestSlot)) }
                coVerify(exactly = 0) { auditor.failed(any()) }

                assertThat(requestSlot.captured.rqMessage).isEqualTo("Hello")
                assertThat(requestSlot.captured.rsMessage).isEqualTo("World")
                assertThat(requestSlot.captured.answerCode).isEqualTo("200")
            }
        }

        @Test
        fun `should treat CancellationException as tolerant and not send failed audit`() = runTest {
            withContext(headersElement + togglesElement) {
                val responses = flow {
                    emit(createInputTranscription("Hello"))
                    emit(createOutputTranscription("World"))
                    throw CancellationException("client cancelled")
                }

                runCatching { accumulator.processResponseChunks(responses).toList() }

                val requestSlot = slot<InteractionAuditRequest>()
                coVerify(exactly = 1) { auditor.success(capture(requestSlot)) }
                coVerify(exactly = 0) { auditor.failed(any()) }

                assertThat(requestSlot.captured.rqMessage).isEqualTo("Hello")
                assertThat(requestSlot.captured.rsMessage).isEqualTo("World")
            }
        }

        @Test
        fun `should send per-turn success audit and session failed audit on RuntimeException`() = runTest {
            withContext(headersElement + togglesElement) {
                val responses = flow {
                    emit(createInputTranscription("Hello"))
                    emit(createOutputTranscription("World"))
                    throw RuntimeException("stream failure")
                }

                runCatching { accumulator.processResponseChunks(responses).toList() }

                val successSlot = slot<InteractionAuditRequest>()
                val failedSlot = slot<InteractionAuditRequest>()
                coVerify(exactly = 1) { auditor.success(capture(successSlot)) }
                coVerify(exactly = 1) { auditor.failed(capture(failedSlot)) }

                assertThat(successSlot.captured.rqMessage).isEqualTo("Hello")
                assertThat(successSlot.captured.rsMessage).isEqualTo("World")

                assertThat(failedSlot.captured.answerCode).isEqualTo("500")
                assertThat(failedSlot.captured.errorCode).isEqualTo("RuntimeException")
                assertThat(failedSlot.captured.errorTitle).isEqualTo("stream failure")
                assertThat(failedSlot.captured.rqMessage).isNull()
                assertThat(failedSlot.captured.rsMessage).isNull()
            }
        }

        @Test
        fun `should send per-turn audits for each completed turn and failed audit on exception`() = runTest {
            withContext(headersElement + togglesElement) {
                val responses = flow {
                    emit(createInputTranscription("Hello"))
                    emit(createOutputTranscription("World"))
                    emit(createInputTranscription("Next"))
                    emit(createOutputTranscription("Response"))
                    throw RuntimeException("stream failure")
                }

                runCatching { accumulator.processResponseChunks(responses).toList() }

                val successRequests = mutableListOf<InteractionAuditRequest>()
                val failedSlot = slot<InteractionAuditRequest>()
                coVerify(exactly = 2) { auditor.success(capture(successRequests)) }
                coVerify(exactly = 1) { auditor.failed(capture(failedSlot)) }

                assertThat(successRequests[0].rqMessage).isEqualTo("Hello")
                assertThat(successRequests[0].rsMessage).isEqualTo("World")
                assertThat(successRequests[1].rqMessage).isEqualTo("Next")
                assertThat(successRequests[1].rsMessage).isEqualTo("Response")

                assertThat(failedSlot.captured.errorCode).isEqualTo("RuntimeException")
            }
        }

        @Test
        fun `should flush incomplete input-only turn on completion`() = runTest {
            withContext(headersElement + togglesElement) {
                val responses = flowOf(
                    createInputTranscription("Hello")
                )

                accumulator.processResponseChunks(responses).toList()

                val requestSlot = slot<InteractionAuditRequest>()
                coVerify(exactly = 1) { auditor.success(capture(requestSlot)) }
                coVerify(exactly = 0) { auditor.failed(any()) }

                assertThat(requestSlot.captured.rqMessage).isEqualTo("Hello")
                assertThat(requestSlot.captured.rsMessage).isEmpty()
            }
        }

        @Test
        fun `should flush incomplete input-only turn with warning on completion`() = runTest {
            withContext(headersElement + togglesElement) {
                val responses = flowOf(
                    createInputTranscription("Hello"),
                    createWarning("warn")
                )

                accumulator.processResponseChunks(responses).toList()

                val requestSlot = slot<InteractionAuditRequest>()
                coVerify(exactly = 1) { auditor.success(capture(requestSlot)) }

                assertThat(requestSlot.captured.rqMessage).isEqualTo("Hello")
                assertThat(requestSlot.captured.rsMessage).isEmpty()
            }
        }

        @Test
        fun `should flush incomplete input-only turn with error on completion`() = runTest {
            withContext(headersElement + togglesElement) {
                val responses = flowOf(
                    createInputTranscription("Hello"),
                    createError(503, "fail")
                )

                accumulator.processResponseChunks(responses).toList()

                val requestSlot = slot<InteractionAuditRequest>()
                coVerify(exactly = 1) { auditor.success(capture(requestSlot)) }

                assertThat(requestSlot.captured.rqMessage).isEqualTo("Hello")
                assertThat(requestSlot.captured.rsMessage).isEmpty()
            }
        }

        @Test
        fun `should flush incomplete output-only turn on completion`() = runTest {
            withContext(headersElement + togglesElement) {
                val responses = flowOf(
                    createOutputTranscription("World")
                )

                accumulator.processResponseChunks(responses).toList()

                val requestSlot = slot<InteractionAuditRequest>()
                coVerify(exactly = 1) { auditor.success(capture(requestSlot)) }

                assertThat(requestSlot.captured.rqMessage).isEmpty()
                assertThat(requestSlot.captured.rsMessage).isEqualTo("World")
            }
        }

        @Test
        fun `should treat StatusException UNAVAILABLE as tolerant`() = runTest {
            withContext(headersElement + togglesElement) {
                val responses = flow {
                    emit(createInputTranscription("Hello"))
                    emit(createOutputTranscription("World"))
                    throw StatusException(Status.UNAVAILABLE.withDescription("RST_STREAM closed stream"))
                }

                runCatching { accumulator.processResponseChunks(responses).toList() }

                coVerify(exactly = 1) { auditor.success(any()) }
                coVerify(exactly = 0) { auditor.failed(any()) }
            }
        }

        @Test
        fun `should treat StatusException CANCELLED as tolerant`() = runTest {
            withContext(headersElement + togglesElement) {
                val responses = flow {
                    emit(createInputTranscription("Hello"))
                    emit(createOutputTranscription("World"))
                    throw StatusException(Status.CANCELLED.withDescription("RPC cancelled"))
                }

                runCatching { accumulator.processResponseChunks(responses).toList() }

                coVerify(exactly = 1) { auditor.success(any()) }
                coVerify(exactly = 0) { auditor.failed(any()) }
            }
        }

        @Test
        fun `should treat StatusException INTERNAL as non-tolerant`() = runTest {
            withContext(headersElement + togglesElement) {
                val responses = flow {
                    emit(createInputTranscription("Hello"))
                    emit(createOutputTranscription("World"))
                    throw StatusException(Status.INTERNAL.withDescription("server error"))
                }

                runCatching { accumulator.processResponseChunks(responses).toList() }

                val failedSlot = slot<InteractionAuditRequest>()
                coVerify(exactly = 1) { auditor.success(any()) }
                coVerify(exactly = 1) { auditor.failed(capture(failedSlot)) }

                assertThat(failedSlot.captured.errorCode).isEqualTo("INTERNAL")
            }
        }
    }

    @Nested
    inner class DialogPublishingTest {

        @Test
        fun `should publish dialog when dialog turn completes`() = runTest {
            withContext(headersElement + togglesElement) {
                val responses = flowOf(
                    createInputTranscription("Hello"),
                    createOutputTranscription("Hi there"),
                    createInputTranscription("Next question")
                )

                accumulator.processResponseChunks(responses).toList()

                coVerify(exactly = 2) { dialogTurnPublisher.publishDialogTurn(any(), any(), any(), any(), any()) }
            }
        }

        @Test
        fun `should publish dialog with accumulated text`() = runTest {
            withContext(headersElement + togglesElement) {
                val inputs = mutableListOf<String>()
                val outputs = mutableListOf<String>()
                coEvery {
                    dialogTurnPublisher.publishDialogTurn(
                        capture(inputs),
                        capture(outputs),
                        any()
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

                assertEquals("How are you?", inputs[0])
                assertEquals("I am fine!", outputs[0])
                assertEquals("Great", inputs[1])
                assertEquals("", outputs[1])
            }
        }

        @Test
        fun `should flush accumulated input on completion when no output arrived`() = runTest {
            withContext(headersElement + togglesElement) {
                val inputs = mutableListOf<String>()
                val outputs = mutableListOf<String>()
                coEvery {
                    dialogTurnPublisher.publishDialogTurn(capture(inputs), capture(outputs), any())
                } returns Unit

                val responses = flowOf(
                    createInputTranscription("First"),
                    createInputTranscription("Second"),
                    createInputTranscription("Third")
                )

                accumulator.processResponseChunks(responses).toList()

                coVerify(exactly = 1) { dialogTurnPublisher.publishDialogTurn(any(), any(), any(), any(), any()) }
                assertEquals("FirstSecondThird", inputs[0])
                assertEquals("", outputs[0])
            }
        }

        @Test
        fun `should publish multiple dialogs for multiple turns`() = runTest {
            withContext(headersElement + togglesElement) {
                val responses = flowOf(
                    createInputTranscription("First"),
                    createOutputTranscription("Answer 1"),
                    createInputTranscription("Second"),
                    createOutputTranscription("Answer 2"),
                    createInputTranscription("Third")
                )

                accumulator.processResponseChunks(responses).toList()

                coVerify(exactly = 3) { dialogTurnPublisher.publishDialogTurn(any(), any(), any(), any(), any()) }
            }
        }

        @Test
        fun `should calculate assistant response time from output generation`() = runTest {
            withContext(headersElement + togglesElement) {
                val responseTimes = mutableListOf<Long>()
                coEvery {
                    dialogTurnPublisher.publishDialogTurn(any(), any(), capture(responseTimes))
                } returns Unit

                val responses = flowOf(
                    createInputTranscription("Hello"),
                    createOutputTranscription("Hi"),
                    createInputTranscription("Next")
                )

                accumulator.processResponseChunks(responses).toList()

                assertThat(responseTimes[0]).isGreaterThanOrEqualTo(0L)
            }
        }

        @Test
        fun `should calculate assistant response time correctly for multiple turns`() = runTest {
            withContext(headersElement + togglesElement) {
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

                assertThat(responseTimes).hasSize(3)
                responseTimes.take(2).forEachIndexed { index, responseTime ->
                    assertThat(responseTime)
                        .describedAs("Assistant response time for turn ${index + 1}")
                        .isGreaterThanOrEqualTo(0L)
                        .isLessThan(10_000L)
                }
            }
        }
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

    private fun togglesElement(
        kapSendExtra: Boolean
    ): VoiceSessionFeatureTogglesElement = VoiceSessionFeatureTogglesElement(
        VoiceSessionFeatureToggles(
            mapOf(KAP_SEND_EXTRA to Parameter(KAP_SEND_EXTRA, kapSendExtra.toString()))
        )
    )

    @Nested
    inner class ExtraPublishingTest {

        @BeforeEach
        fun enableKapExtra() {
            togglesElement = togglesElement(kapSendExtra = true)
        }

        @Test
        fun `should publish extra with timestamps on dialog turn`() = runTest {
            withContext(headersElement + togglesElement) {
                val responses = flowOf(
                    createInputTranscription("Hello"),
                    createOutputTranscription("Hi"),
                    createInputTranscription("Next")
                )

                val extras = mutableListOf<DialogTurnExtra?>()
                coEvery {
                    dialogTurnPublisher.publishDialogTurn(
                        any(), any(), any(), captureNullable(extras), any()
                    )
                } returns Unit

                accumulator.processResponseChunks(responses).toList()

                val extra = extras[0]
                assertThat(extra).isNotNull
                assertThat(extra!!.userMessageEndTS).isNotNull()
            }
        }

        @Test
        fun `should publish extra with audio timestamps`() = runTest {
            withContext(headersElement + togglesElement) {
                val extras = mutableListOf<DialogTurnExtra?>()
                coEvery {
                    dialogTurnPublisher.publishDialogTurn(
                        any(), any(), any(), captureNullable(extras), any()
                    )
                } returns Unit

                accumulator.processRequestChunks(
                    flowOf(VoiceRequest.Audio(AudioContent()))
                ).toList()

                val responses = flowOf(
                    createInputTranscription("Hello"),
                    VoiceResponse.Output(ContentFromModel.Audio(AudioOutput(byteArrayOf(1)))),
                    createOutputTranscription("Hi"),
                    VoiceResponse.Output(
                        ContentFromModel.Audio(AudioOutput(byteArrayOf(2), isFinal = true))
                    ),
                    createInputTranscription("Next")
                )
                accumulator.processResponseChunks(responses).toList()

                val extra = extras[0]
                assertThat(extra).isNotNull
                assertThat(extra!!.userMessageStartTS).isNotNull()
                assertThat(extra.userMessageEndTS).isNotNull()
                assertThat(extra.assistantMessageStartTS).isNotNull()
                assertThat(extra.assistantMessageEndTS).isNotNull()
                assertThat(extra.userMessageStartTS!!)
                    .isLessThan(extra.assistantMessageStartTS!!)
                assertThat(extra.assistantMessageStartTS!!)
                    .isLessThan(extra.assistantMessageEndTS!!)
            }
        }

        @Test
        fun `should publish extra with warning and error events`() = runTest {
            withContext(headersElement + togglesElement) {
                val extras = mutableListOf<DialogTurnExtra?>()
                coEvery {
                    dialogTurnPublisher.publishDialogTurn(
                        any(), any(), any(), captureNullable(extras), any()
                    )
                } returns Unit

                val responses = flowOf(
                    createInputTranscription("Hello"),
                    createWarning("low confidence"),
                    createError(400, "Bad Request"),
                    createOutputTranscription("Sorry"),
                    createInputTranscription("Next")
                )
                accumulator.processResponseChunks(responses).toList()

                val extra = extras[0]
                assertThat(extra).isNotNull
                assertThat(extra!!.events).hasSize(2)

                val warning = extra.events.find { it.eventName == "warning" }
                assertThat(warning).isNotNull
                assertThat((warning!!.payload as WarningPayload).message)
                    .isEqualTo("low confidence")

                val error = extra.events.find { it.eventName == "error" }
                assertThat(error).isNotNull
                val errorPayload = error!!.payload as ErrorPayload
                assertThat(errorPayload.status).isEqualTo(400)
                assertThat(errorPayload.message).isEqualTo("Bad Request")
            }
        }

        @Test
        fun `should publish extra with function_call event`() = runTest {
            withContext(headersElement + togglesElement) {
                val extras = mutableListOf<DialogTurnExtra?>()
                coEvery {
                    dialogTurnPublisher.publishDialogTurn(
                        any(), any(), any(), captureNullable(extras), any()
                    )
                } returns Unit

                val responses = flowOf(
                    createInputTranscription("Check balance"),
                    VoiceResponse.FunctionCalling(
                        FunctionCallingData(
                            FunctionCall("get_balance", """{"id":"1"}"""),
                            timestamp = 999L
                        )
                    ),
                    createOutputTranscription("Your balance is 1000"),
                    createInputTranscription("Thanks")
                )
                accumulator.processResponseChunks(responses).toList()

                val extra = extras[0]
                assertThat(extra).isNotNull

                val fcEvent = extra!!.events.find { it.eventName == "function_call" }
                assertThat(fcEvent).isNotNull
                val fcPayload = fcEvent!!.payload as FunctionCallPayload
                assertThat(fcPayload.functionCall.name).isEqualTo("get_balance")
                assertThat(fcPayload.timestamp).isEqualTo(999L)
            }
        }

        @Test
        fun `should reset extra state between turns`() = runTest {
            withContext(headersElement + togglesElement) {
                val extras = mutableListOf<DialogTurnExtra?>()
                coEvery {
                    dialogTurnPublisher.publishDialogTurn(
                        any(), any(), any(), captureNullable(extras), any()
                    )
                } returns Unit

                val responses = flowOf(
                    createInputTranscription("First"),
                    createWarning("warn1"),
                    createOutputTranscription("Answer 1"),
                    createInputTranscription("Second"),
                    createOutputTranscription("Answer 2"),
                    createInputTranscription("Third")
                )
                accumulator.processResponseChunks(responses).toList()

                assertThat(extras).hasSize(3)
                assertThat(extras[0]!!.events).hasSize(1)
                assertThat(extras[1]!!.events).isEmpty()
                assertThat(extras[2]!!.events).isEmpty()
            }
        }

        @Test
        fun `should not publish extra when toggle is disabled`() = runTest {
            togglesElement = togglesElement(kapSendExtra = false)
            withContext(headersElement + togglesElement) {
                val extras = mutableListOf<DialogTurnExtra?>()
                coEvery {
                    dialogTurnPublisher.publishDialogTurn(
                        any(), any(), any(), captureNullable(extras), any()
                    )
                } returns Unit

                val responses = flowOf(
                    createInputTranscription("Hello"),
                    createWarning("warn"),
                    createOutputTranscription("Hi"),
                    createInputTranscription("Next")
                )
                accumulator.processResponseChunks(responses).toList()

                extras.forEach { assertThat(it).isNull() }
            }
        }
    }
}
