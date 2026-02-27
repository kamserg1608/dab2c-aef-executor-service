package ru.sbrf.dab2c.executor.library.audit.model

/**
 * Common schema keys and default codes for audit messages.
 */
object AuditMessageSchema {

    const val ANSWER_CODE_OK: String = "200"
    const val ANSWER_CODE_FAIL: String = "500"

    const val DEFAULT_ERROR_TITLE: String = "Call error"

    // -------- Voice stream errors --------

    const val ERROR_CODE_VOICE_REQUEST_STREAM: String = "VOICE_REQUEST_STREAM_ERROR"
    const val ERROR_CODE_VOICE_RESPONSE_STREAM: String = "VOICE_RESPONSE_STREAM_ERROR"
    const val ERROR_TITLE_VOICE_STREAM: String = "Voice stream error"

    // -------- GigaVoice errors --------

    const val ERROR_CODE_GIGAVOICE_SETTINGS: String = "GIGAVOICE_SETTINGS_ERROR"
    const val ERROR_CODE_GIGAVOICE_FUNCTION: String = "GIGAVOICE_FUNCTION_ERROR"

    // -------- Common JSON keys --------

    const val KEY_ENDPOINT: String = "endpoint"
    const val KEY_RECEIVER: String = "receiver"
    const val KEY_CONVERSATION_ID: String = "conversationId"
    const val KEY_EDU_ID: String = "eduId"
    const val KEY_UFS_SESSION: String = "ufsSession"
    const val KEY_CHANNEL: String = "channel"
    const val KEY_AGENT_CONFIGURATION: String = "agentConfiguration"
    const val KEY_VOICE_SETTINGS: String = "voiceSettings"
    const val KEY_FUNCTION_CALLING: String = "functionCalling"
    const val KEY_CONTEXT_DATA: String = "contextData"

    const val KEY_TYPE: String = "type"
    const val TYPE_AUDIO: String = "Audio"
    const val KEY_SPEECH_START: String = "speechStart"
    const val KEY_SPEECH_END: String = "speechEnd"
    const val KEY_AUDIO_PRESENT: String = "audioChunkPresent"
    const val KEY_AUDIO_SIZE: String = "audioChunkSize"
}
