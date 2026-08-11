package ru.sbrf.dab2c.executor.clients.efs.adapter.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SDS model for session info deserialization.
 */
data class SdsSessionInfo(
    @JsonProperty("meta")
    val meta: SdsSessionMeta,
    @JsonProperty("common")
    val common: SdsSessionCommon,
    @JsonProperty("user_info")
    val userInfo: SdsSessionUserInfo
)

/**
 * SDS model for session metadata.
 */
data class SdsSessionMeta(
    @JsonProperty("session_id")
    val sessionId: String,
    @JsonProperty("user_id")
    val userId: String,
    @JsonProperty("ucp_id")
    val ucpId: String?,
    @JsonProperty("ufs_host")
    val ufsHost: String,
    @JsonProperty("is_valid")
    val isValid: Boolean = true
)

/**
 * SDS model for common session data.
 */
data class SdsSessionCommon(
    @JsonProperty("block")
    val block: String,
    @JsonProperty("channel")
    val channel: String,
    @JsonProperty("surface")
    val surface: String,
    @JsonProperty("platform")
    val platform: String,
    @JsonProperty("sdk_version")
    val sdkVersion: String,
    @JsonProperty("entry_point")
    val entryPoint: String,
    @JsonProperty("app_version")
    val appVersion: String,
    @JsonProperty("channel_version")
    val channelVersion: String,
    @JsonProperty("app_source")
    val appSource: String,
    @JsonProperty("time_zone")
    val timeZone: String
)

/**
 * SDS model for session user info.
 */
data class SdsSessionUserInfo(
    @JsonProperty("first_name")
    val firstName: String,
    @JsonProperty("patr_name")
    val patrName: String,
    @JsonProperty("birth_day")
    val birthDay: String,
    @JsonProperty("segment_code_type")
    val segmentCodeType: String,
    @JsonProperty("ucp_id")
    val ucpId: String?
)
