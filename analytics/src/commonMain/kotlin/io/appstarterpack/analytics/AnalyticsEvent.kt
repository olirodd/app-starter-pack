package io.appstarterpack.analytics

data class AnalyticsEvent(
    val name: String,
    val parameters: Map<String, String> = emptyMap()
)
