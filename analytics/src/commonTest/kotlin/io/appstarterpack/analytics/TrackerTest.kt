package io.appstarterpack.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackerTest {

    private class RecordingAnalyticsClient : AnalyticsClient {
        val events = mutableListOf<AnalyticsEvent>()
        val users = mutableListOf<AnalyticsUser>()
        override fun track(event: AnalyticsEvent) { events += event }
        override fun identify(user: AnalyticsUser) { users += user }
    }

    private class RecordingErrorReporter : ErrorReporter {
        val errors = mutableListOf<Throwable>()
        val metadataList = mutableListOf<Map<String, String>>()
        override fun report(throwable: Throwable, metadata: Map<String, String>) {
            errors += throwable
            metadataList += metadata
        }
    }

    // --- Fan-out ---

    @Test
    fun `track dispatches event to all clients`() {
        val a = RecordingAnalyticsClient()
        val b = RecordingAnalyticsClient()
        val tracker = Tracker(clients = listOf(a, b), reporters = emptyList())
        val event = AnalyticsEvent("screen_view", mapOf("screen" to "home"))

        tracker.track(event)

        assertEquals(listOf(event), a.events)
        assertEquals(listOf(event), b.events)
    }

    @Test
    fun `identify dispatches user to all clients`() {
        val a = RecordingAnalyticsClient()
        val b = RecordingAnalyticsClient()
        val tracker = Tracker(clients = listOf(a, b), reporters = emptyList())
        val user = AnalyticsUser("user-123", mapOf("plan" to "pro"))

        tracker.identify(user)

        assertEquals(listOf(user), a.users)
        assertEquals(listOf(user), b.users)
    }

    @Test
    fun `report dispatches error to all reporters`() {
        val a = RecordingErrorReporter()
        val b = RecordingErrorReporter()
        val tracker = Tracker(clients = emptyList(), reporters = listOf(a, b))
        val error = RuntimeException("something went wrong")

        tracker.report(error)

        assertEquals(listOf<Throwable>(error), a.errors)
        assertEquals(listOf<Throwable>(error), b.errors)
    }

    @Test
    fun `report passes metadata to all reporters`() {
        val a = RecordingErrorReporter()
        val b = RecordingErrorReporter()
        val tracker = Tracker(clients = emptyList(), reporters = listOf(a, b))
        val metadata = mapOf("endpoint" to "categories", "status_code" to "500")

        tracker.report(RuntimeException("error"), metadata)

        assertEquals(listOf(metadata), a.metadataList)
        assertEquals(listOf(metadata), b.metadataList)
    }

    // --- Consent ---

    @Test
    fun `track does nothing when analytics disabled`() {
        val client = RecordingAnalyticsClient()
        val tracker = Tracker(clients = listOf(client), reporters = emptyList())
        tracker.analyticsEnabled = false

        tracker.track(AnalyticsEvent("button_tap"))

        assertTrue(client.events.isEmpty())
    }

    @Test
    fun `identify does nothing when analytics disabled`() {
        val client = RecordingAnalyticsClient()
        val tracker = Tracker(clients = listOf(client), reporters = emptyList())
        tracker.analyticsEnabled = false

        tracker.identify(AnalyticsUser("user-123"))

        assertTrue(client.users.isEmpty())
    }

    @Test
    fun `report still works when analytics disabled`() {
        val reporter = RecordingErrorReporter()
        val tracker = Tracker(clients = emptyList(), reporters = listOf(reporter))
        tracker.analyticsEnabled = false
        val error = RuntimeException("crash")

        tracker.report(error)

        assertEquals(listOf<Throwable>(error), reporter.errors)
    }

    @Test
    fun `re-enabling analytics allows tracking`() {
        val client = RecordingAnalyticsClient()
        val tracker = Tracker(clients = listOf(client), reporters = emptyList())
        tracker.analyticsEnabled = false
        tracker.track(AnalyticsEvent("ignored"))
        tracker.analyticsEnabled = true
        val event = AnalyticsEvent("button_tap")

        tracker.track(event)

        assertEquals(listOf(event), client.events)
    }

    // --- Empty lists ---

    @Test
    fun `empty clients list is safe`() {
        val tracker = Tracker(clients = emptyList(), reporters = emptyList())
        tracker.track(AnalyticsEvent("test"))
        tracker.identify(AnalyticsUser("user-123"))
    }

    @Test
    fun `empty reporters list is safe`() {
        val tracker = Tracker(clients = emptyList(), reporters = emptyList())
        tracker.report(RuntimeException("test"))
    }
}
