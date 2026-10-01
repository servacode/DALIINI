package com.servacode.directory.core.analytics

/**
 * The one thing the sender needs from the outside world: a way to post an event and be told what
 * happened. Expressed as an interface here so `core:analytics` does not depend on the network
 * module, and so the sender's behaviour can be tested against a fake that never opens a socket.
 */
interface AnalyticsTransport {
    /**
     * Posts one event.
     *
     * Returns the HTTP status the server answered with, or null when there was no answer at all —
     * no network, a refused connection, a timeout. Implementations must not throw: a measurement
     * that can throw is a measurement that can crash the app it is measuring.
     */
    suspend fun send(name: String, properties: Map<String, String>, anonymousId: String): Int?
}

/**
 * Where the pseudonymous id comes from.
 *
 * It identifies a device to itself across launches so "how many people" is answerable, and it is
 * nothing else: not an account, not a phone number, not derived from any hardware identifier.
 * Clearing the app's data gives a new one, which is the behaviour somebody deleting their data
 * would expect.
 */
interface AnonymousId {
    suspend fun current(): String
}
