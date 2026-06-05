package ru.sbrf.dab2c.executor.clients.common.util

/**
 * Builds a full URL by combining a base URL with an endpoint path.
 * Handles trailing slashes on base URL and leading slashes on endpoint to avoid double slashes
 * or missing slashes in the combined URL.
 */
fun buildFullUrl(baseUrl: String, endpoint: String): String {
    val normalizedBase = baseUrl.trimEnd('/')
    val normalizedEndpoint = if (endpoint.startsWith('/')) endpoint else "/$endpoint"
    return normalizedBase + normalizedEndpoint
}
