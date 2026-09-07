package ru.sbrf.dab2c.executor.voice.postprocess

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.model.PostProcessResult
import ru.sbrf.dab2c.executor.voice.config.properties.VoiceExecutorConfiguration.Companion.POST_PROCESS_SCOPE_BEAN_NAME
import ru.sbrf.dab2c.executor.voice.service.api.AnalyticsPublisher
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

/** Runs `/postprocess` calls on a scope that outlives the sessions that produced them. */
@Component
class PostProcessRunner(
    @Qualifier(POST_PROCESS_SCOPE_BEAN_NAME) private val scope: CoroutineScope,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient,
    private val analyticsPublisher: AnalyticsPublisher
) {

    /** Never suspends. */
    fun submit(snapshot: PostProcessSnapshot, context: CoroutineContext) {
        scope.launch(context) { execute(snapshot) }
    }

    private suspend fun execute(snapshot: PostProcessSnapshot) {
        val result = try {
            gigaVoiceAgentClient.postProcess(
                conversationId = snapshot.conversationId,
                agentConfiguration = snapshot.agentConfiguration,
                contextData = snapshot.contextData
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Post-processing of conversation ${snapshot.conversationId} failed" }
            return
        }

        logger.info {
            "Post-processing of conversation ${snapshot.conversationId} returned " +
                "${result.analytics.size} analytics entries"
        }
        publish(snapshot, result)
    }

    private suspend fun publish(snapshot: PostProcessSnapshot, result: PostProcessResult) {
        try {
            analyticsPublisher.publishAnalytics(
                analytics = result.analytics,
                requestId = null,
                conversationId = snapshot.conversationId,
                agentConfiguration = snapshot.agentConfiguration,
                assistantMessageId = snapshot.assistantMessageId
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Publishing analytics of conversation ${snapshot.conversationId} failed" }
        }
    }
}
