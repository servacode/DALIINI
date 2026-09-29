package com.servacode.directory.core.model

import java.net.URI

/** Where a link into the app leads, once it has been read. */
sealed interface DeepLinkTarget {
    /** `https://<host>/f/{id}`: one facility's page. */
    data class Facility(val id: String) : DeepLinkTarget

    /** `https://<host>/duty` (and the site's `/duty/today`): who is on duty. */
    data object DutyNow : DeepLinkTarget

    /** `https://<host>/{provinceCode}`: choose that province and open Home. */
    data class Province(val code: String) : DeepLinkTarget
}

/**
 * Reads the site's links as the app's own places. Pure, so every accepted and refused shape is
 * tested without Android.
 *
 * Only `https` on one of [hosts] is accepted (a `www.` prefix too); anything else — another
 * host, another scheme, a path the app does not know — is null and the app simply opens as it
 * would from its icon. Identifiers are checked for shape so a crafted link cannot smuggle text
 * into a route.
 */
object DeepLinks {
    private val FACILITY_ID = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private val PROVINCE_CODE = Regex("^[a-z][a-z0-9-]{1,39}$")

    /**
     * Paths the site uses for its own pages, which are not province codes (`apps/web/app`), and
     * a few it may yet.
     */
    private val RESERVED = setOf(
        "f", "duty", "about", "privacy", "terms", "help", "search", "map", "api", "admin", "static",
        "media", "well-known", "sitemap.xml", "robots.txt", "faq", "how-we-verify", "owners",
        "support", "delete-account",
    )

    fun parse(link: String?, hosts: Set<String>): DeepLinkTarget? {
        val uri = runCatching { URI(link?.trim() ?: return null) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
        if (hosts.none { it.lowercase().removePrefix("www.") == host }) return null
        val segments = uri.path.orEmpty().split('/').filter { it.isNotBlank() }
        return when {
            segments.size == 2 && segments[0] == "f" ->
                segments[1].takeIf { FACILITY_ID.matches(it) }?.let { DeepLinkTarget.Facility(it.lowercase()) }
            segments.firstOrNull() == "duty" && (segments.size == 1 || segments == listOf("duty", "today")) ->
                DeepLinkTarget.DutyNow
            segments.size == 1 && segments[0].lowercase() !in RESERVED ->
                segments[0].lowercase().takeIf { PROVINCE_CODE.matches(it) }?.let(DeepLinkTarget::Province)
            else -> null
        }
    }
}
