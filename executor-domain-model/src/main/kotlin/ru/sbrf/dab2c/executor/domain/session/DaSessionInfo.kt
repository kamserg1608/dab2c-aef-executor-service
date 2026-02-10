package ru.sbrf.dab2c.executor.domain.session

/**
 * Session memory data.
 */
data class DaSessionInfo(
    val meta: DaSessionMeta,
    val common: DaSessionCommon,
    val userInfo: DaSessionUserInfo
)

/**
 * Session metadata.
 */
data class DaSessionMeta(
    val sessionId: String,
    val userId: String,
    val ucpId: String,
    val ufsHost: String,
    val isValid: Boolean = true
)

/**
 * Common session data.
 */
data class DaSessionCommon(
    val block: String? = null,
    val channel: String,
    val surface: String? = null,
    val platform: String? = null,
    val sdkVersion: String? = null,
    val entryPoint: String? = null,
    val appVersion: String? = null,
    val channelVersion: String? = null,
    val appSource: String? = null,
    val timeZone: String? = null
)

/**
 * Session user info.
 */
data class DaSessionUserInfo(
    val firstName: String? = null,
    val patrName: String? = null,
    val birthDay: String? = null,
    val segmentCodeType: String? = "0",
    val ucpId: String? = null
)
