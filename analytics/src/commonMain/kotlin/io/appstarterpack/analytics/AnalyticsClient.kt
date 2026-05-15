package io.appstarterpack.analytics

interface AnalyticsClient {
    fun track(event: AnalyticsEvent)
    fun identify(user: AnalyticsUser)
}
