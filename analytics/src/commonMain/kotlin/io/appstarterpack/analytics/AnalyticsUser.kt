package io.appstarterpack.analytics

data class AnalyticsUser(
    val id: String,
    val properties: Map<String, String> = emptyMap()
)
