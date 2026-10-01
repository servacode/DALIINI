package com.servacode.directory.core.analytics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The tracker the app actually uses.
 *
 * `track` does one thing: it drops the event into a buffer and returns. It does not suspend, it
 * does not touch the network, and it cannot fail — a screen calls it between two lines of its own
 * work and is not delayed by a byte. A single worker drains the buffer on its own scope.
 *
 * **What happens to an event that does not get through.** A phone in a basement is the normal
 * case, not the exceptional one, so an event the server never answered is held and tried again
 * the next time there is something to send. The held list is bounded and drops the oldest, so a
 * long trip underground costs the first few measurements and never memory. An event the server
 * *refused* is thrown away immediately: a 400 means the registry does not know it, which will be
 * just as true in ten minutes, and retrying it is a loop against our own server.
 *
 * Both of those decisions live in [outcomeFor] and [enqueueBounded], which are pure functions
 * with their own tests — this class is only the plumbing around them.
 */
@Singleton
class QueuedAnalyticsTracker @Inject constructor(
    private val transport: AnalyticsTransport,
    private val anonymousId: AnonymousId,
    scope: CoroutineScope,
) : AnalyticsTracker {

    private val events = MutableSharedFlow<AnalyticsEvent>(
        extraBufferCapacity = ANALYTICS_QUEUE_LIMIT,
        // The alternative is suspending the caller, which means a screen waiting on a
        // measurement of itself. Losing the oldest few is the cheaper of the two.
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Held events, oldest first. Touched only by the single collector below. */
    private var pending: List<AnalyticsEvent> = emptyList()

    init {
        scope.launch {
            events.collect { event ->
                // Nothing about measuring may reach the app, including an exception from a
                // transport that was supposed not to throw.
                runCatching {
                    val id = anonymousId.current()
                    pending = drain(pending + event, id)
                }
            }
        }
    }

    override fun track(event: AnalyticsEvent) {
        events.tryEmit(event)
    }

    /**
     * Sends what it can and returns what is left to try again.
     *
     * It stops at the first event the network could not deliver: if one did not get through, the
     * ones behind it will not either, and hammering a dead connection in a loop is what drains a
     * battery. Everything after the stop stays held, in order.
     */
    private suspend fun drain(queue: List<AnalyticsEvent>, id: String): List<AnalyticsEvent> {
        var index = 0
        while (index < queue.size) {
            val event = queue[index]
            val status = runCatching { transport.send(event.name, event.properties, id) }
                .getOrNull()
            when (outcomeFor(status)) {
                AnalyticsOutcome.SENT, AnalyticsOutcome.DROP -> index += 1
                AnalyticsOutcome.RETRY -> return bound(queue.drop(index))
            }
        }
        return emptyList()
    }

    private fun bound(queue: List<AnalyticsEvent>): List<AnalyticsEvent> =
        if (queue.size <= ANALYTICS_QUEUE_LIMIT) queue else queue.takeLast(ANALYTICS_QUEUE_LIMIT)
}
