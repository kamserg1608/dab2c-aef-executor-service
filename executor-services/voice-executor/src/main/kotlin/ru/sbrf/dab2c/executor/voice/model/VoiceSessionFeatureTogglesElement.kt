package ru.sbrf.dab2c.executor.voice.model

import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.CoroutineContext

/** Coroutine context element carrying [VoiceSessionFeatureToggles]. */
class VoiceSessionFeatureTogglesElement(
    val featureToggles: VoiceSessionFeatureToggles
) : CoroutineContext.Element {
    override val key get() = Key

    /** Coroutine context key. */
    companion object Key : CoroutineContext.Key<VoiceSessionFeatureTogglesElement>
}

/** Returns [VoiceSessionFeatureToggles] from the current coroutine context or throws. */
suspend fun currentFeatureToggles(): VoiceSessionFeatureToggles =
    currentCoroutineContext()[VoiceSessionFeatureTogglesElement]?.featureToggles
        ?: error("VoiceSessionFeatureTogglesElement not found in coroutine context")
