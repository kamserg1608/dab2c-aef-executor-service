package ru.sbrf.dab2c.executor.voice.service.impl

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCall
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionResult
import ru.sbrf.dab2c.executor.clients.iag.api.IagFunctionClient
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.domain.voice.FunctionOptions
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.VoiceSession
import ru.sbrf.dab2c.executor.voice.service.api.AnalyticsPublisher
import ru.sbrf.dab2c.executor.voice.service.impl.DialogContextTestFixtures.CONVERSATION_ID
import ru.sbrf.dab2c.executor.voice.service.impl.DialogContextTestFixtures.agentConfiguration
import ru.sbrf.dab2c.executor.voice.service.impl.DialogContextTestFixtures.dialogContext
import ru.sbrf.dab2c.executor.voice.service.impl.DialogContextTestFixtures.sessionContext
import java.util.concurrent.Executors

/**
 * Verifies how the context returned by /functions updates the serving state and reaches the next call.
 */
class FunctionCallServiceImplTest {

    private val ivrContext = dialogContext("""{"ivr":true}""")

    private val session = VoiceSession(state = MutableStateFlow(servingState()))
    private val gigaVoiceAgentClient: GigaVoiceAgentClient = mockk()
    private val sentContexts = mutableListOf<DialogContext>()

    private val service = FunctionCallServiceImpl(
        session = session,
        gigaVoiceAgentClient = gigaVoiceAgentClient,
        iagFunctionClient = mockk<IagFunctionClient>(),
        configuratorClient = mockk<ConfiguratorClient>(),
        analyticsPublisher = mockk<AnalyticsPublisher>(relaxed = true)
    )

    @Test
    fun `should keep the current context when the agent returns none`() = runTest {
        stubFunctions(emptyMap())

        call(FIRST_FUNCTION)

        assertThat(sentContexts).containsExactly(ivrContext)
        assertThat(servingContext()).isEqualTo(ivrContext)
    }

    @Test
    fun `should replace the context and pass the updated one to a function already queued behind it`() = runTest {
        val updated = dialogContext("""{"a":1}""")
        stubFunctions(mapOf(FIRST_FUNCTION to updated))

        Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { worker ->
            withContext(sessionContext() + worker) {
                service.callFunction(callOf(FIRST_FUNCTION))
                service.callFunction(callOf(SECOND_FUNCTION))
            }
            repeat(2) { session.callbackChannels.downstream.receive() }
        }

        assertThat(sentContexts).containsExactly(ivrContext, updated)
        assertThat(servingContext()).isEqualTo(updated)
    }

    private fun stubFunctions(contexts: Map<String, DialogContext>) {
        coEvery { gigaVoiceAgentClient.executeFunctionCall(any(), any(), any(), any()) } answers {
            val requested = arg<FunctionCalling>(2).functionCall.name
            sentContexts += arg<DialogContext>(3)
            FunctionCallResult(
                result = functionResult {
                    content = """{"ok":true}"""
                    functionName = requested
                },
                context = contexts[requested]
            )
        }
    }

    private suspend fun call(functionName: String) {
        withContext(sessionContext()) { service.callFunction(callOf(functionName)) }
        session.callbackChannels.downstream.receive()
    }

    private fun servingContext() = (session.state.value as ProcessingState.Serving).contextData

    private fun callOf(functionName: String) = functionCalling {
        functionCall = functionCall {
            name = functionName
            arguments = """{"id":"1"}"""
        }
        timestamp = 1000L
    }

    private fun servingState() = ProcessingState.Serving(
        contextData = ivrContext,
        agentConfiguration = agentConfiguration,
        conversationId = CONVERSATION_ID,
        functionRegistry = FunctionPerformers(
            mapOf(
                FIRST_FUNCTION to FunctionOptions(isBackendFunction = true),
                SECOND_FUNCTION to FunctionOptions(isBackendFunction = true)
            )
        )
    )

    private companion object {
        const val FIRST_FUNCTION = "get_account_balance"
        const val SECOND_FUNCTION = "check_transaction_status"
    }
}
