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
    val block: String,
    val channel: String,
    val surface: String,
    val platform: String,
    val sdkVersion: String,
    val entryPoint: String,
    val appVersion: String,
    val channelVersion: String,
    val appSource: String,
    val timeZone: String
)

/**
 * Session user info.
 */
data class DaSessionUserInfo(
    val firstName: String,
    val patrName: String,
    val birthDay: String,
    val segmentCodeType: String,
    val ucpId: String
)
