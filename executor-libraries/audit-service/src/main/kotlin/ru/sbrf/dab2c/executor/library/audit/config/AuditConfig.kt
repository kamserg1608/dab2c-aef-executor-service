package ru.sbrf.dab2c.executor.library.audit.config

/**
 * Static audit configuration for DAB2C integration.
 *
 * These constants must match EFS metamodel definition.
 */
object AuditConfig {

    /**
     * Master switch for audit sending.
     */
    const val ENABLED: Boolean = true

    /**
     * EFS metamodel version.
     */
    const val METAMODEL_VERSION: String = "1.0"

    /**
     * EFS module name.
     */
    const val MODULE: String = "CI09677600_ivr_human_agent"

    /**
     * Sender service identifier.
     */
    const val SENDER: String = "VOICE_EXECUTOR"

    /**
     * Default receiver.
     */
    const val DEFAULT_RECEIVER: String = "VOICE_RECEIVER"
}
