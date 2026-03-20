package ru.sbrf.dab2c.executor.voice.model

import ru.sbrf.dab2c.executor.clients.efs.adapter.api.Parameter

/**
 * Feature toggles resolved from EFS parameters at the start of a voice session.
 *
 * Toggles are static for the lifetime of a session: resolved once during initialization
 * and placed into the coroutine context via [VoiceSessionFeatureTogglesElement].
 * Access them anywhere in the session scope with [currentFeatureToggles] — the value is
 * returned immediately from the map, so there is no need to cache it in local fields.
 */
data class VoiceSessionFeatureToggles(private val parameters: Map<String, Parameter>) {

    /** Whether to send extra data to KAP. */
    val kapSendExtra: Boolean
        get() = parameters.getValue(KAP_SEND_EXTRA).getBool { KAP_SEND_EXTRA_DEFAULT }

    /** Toggle parameter name constants and defaults. */
    companion object {
        const val KAP_SEND_EXTRA = "aef.executor.toggles.kap.send.extra"
        private const val KAP_SEND_EXTRA_DEFAULT = false

        /** All parameter names to query from EFS. */
        val PARAMETER_NAMES: List<String> = listOf(KAP_SEND_EXTRA)
    }
}
