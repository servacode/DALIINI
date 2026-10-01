package com.servacode.directory.core.analytics

/**
 * The rules an event sender follows, written so they can be tested without a network, a device
 * or a clock.
 *
 * A product measurement must never be able to harm the thing it measures. Three rules follow
 * from that, and all three are here rather than in the sender so they can be proven:
 *
 *  - **Nothing is ever retried forever.** A phone in a basement would otherwise build a queue
 *    that grows for as long as the app is open.
 *  - **The queue is bounded, and it drops the oldest.** Keeping the newest is the right way
 *    round: a product question is about what people are doing now.
 *  - **A failure is not an error.** Anything the server will never accept — an unknown name, a
 *    key it does not declare — is dropped rather than retried, because retrying it is a loop.
 */

/** How many unsent events are kept before the oldest is dropped. */
const val ANALYTICS_QUEUE_LIMIT = 50

/** What the sender should do with an event after an attempt. */
enum class AnalyticsOutcome {
    /** Accepted; forget it. */
    SENT,

    /** The server will never accept it. Dropping it is the only way out of the loop. */
    DROP,

    /** Something temporary — no network, a 5xx. Keep it for the next attempt. */
    RETRY,
}

/**
 * How an HTTP status maps onto what to do next.
 *
 * 4xx is the app's own fault and will be its fault again next time, so it is dropped — with one
 * exception: 429 means "not now", which is the definition of retry. A refused connection has no
 * status at all and is temporary by nature.
 */
fun outcomeFor(status: Int?): AnalyticsOutcome = when {
    status == null -> AnalyticsOutcome.RETRY
    status in 200..299 -> AnalyticsOutcome.SENT
    status == 429 -> AnalyticsOutcome.RETRY
    status in 400..499 -> AnalyticsOutcome.DROP
    else -> AnalyticsOutcome.RETRY
}

/**
 * Adds one event to a pending queue, dropping the oldest once the queue is full.
 *
 * Returns a new list rather than mutating, so the caller holds one immutable value and there is
 * nothing to synchronise around.
 */
fun <T> enqueueBounded(pending: List<T>, event: T, limit: Int = ANALYTICS_QUEUE_LIMIT): List<T> {
    val next = pending + event
    return if (next.size <= limit) next else next.takeLast(limit)
}
