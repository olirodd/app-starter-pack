package io.appstarterpack.analytics

class Tracker(
    private val clients: List<AnalyticsClient>,
    private val reporters: List<ErrorReporter>
) {
    var analyticsEnabled: Boolean = true

    fun track(event: AnalyticsEvent) {
        if (!analyticsEnabled) return
        clients.forEach { it.track(event) }
    }

    fun identify(user: AnalyticsUser) {
        if (!analyticsEnabled) return
        clients.forEach { it.identify(user) }
    }

    fun report(throwable: Throwable) {
        reporters.forEach { it.report(throwable) }
    }
}
