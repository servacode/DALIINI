package com.servacode.directory.core.transport

/**
 * Which app is talking (DECISION-091). The backend records sessions, push tokens and the release
 * a reader is told to update to per platform, and accepts both values for each.
 */
enum class ClientPlatform(val wire: String) {
    ANDROID("ANDROID"),
    IOS("IOS"),
}
