package com.servacode.directory.core.model

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
        val url = HttpsLink.parse(link?.trim() ?: return null) ?: return null
        val host = url.host.removePrefix("www.")
        if (hosts.none { it.lowercase().removePrefix("www.") == host }) return null
        val segments = url.segments
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

/**
 * An `https` link split into its lower-cased host and its decoded path segments: the little of a
 * URL a deep link needs, in common code (DECISION-086). Anything that is not a well-formed
 * absolute `https` link with a host is null, as `java.net.URI` refused it before: a character a
 * URL cannot carry (a space, `<`, a quote), a malformed `%` escape, no host, another scheme.
 */
internal class HttpsLink private constructor(val host: String, val segments: List<String>) {
    companion object {
        private const val PREFIX = "https://"

        // What a URL may carry: RFC 3986's characters, and beyond ASCII anything that is not a
        // control or a space, as java.net.URI allowed.
        private val ALLOWED = Regex(
            "^[A-Za-z0-9\\-._~:/?#@!$&'()*+,;=%" +
                "\\u00A1-\\u1FFF\\u200B-\\u2027\\u202A-\\u202E\\u2030-\\u205E\\u2060-\\u2FFF\\u3001-\\uFFFF]*$",
        )
        private val HOST = Regex("^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*$")
        private val ESCAPE = Regex("%([0-9A-Fa-f]{2})")

        fun parse(link: String): HttpsLink? {
            if (!link.startsWith(PREFIX, ignoreCase = true) || !ALLOWED.matches(link)) return null
            val rest = link.substring(PREFIX.length).substringBefore('#')
            val authorityEnd = rest.indexOfAny(charArrayOf('/', '?')).let { if (it < 0) rest.length else it }
            val hostAndPort = rest.substring(0, authorityEnd).substringAfterLast('@')
            if (hostAndPort.substringAfter(':', "").any { !it.isDigit() }) return null
            val host = hostAndPort.substringBefore(':').lowercase()
            if (!HOST.matches(host)) return null
            val path = rest.substring(authorityEnd).substringBefore('?')
            val segments = path.split('/').map { decode(it) ?: return null }
            return HttpsLink(host, segments.filter { it.isNotBlank() })
        }

        /** Percent-decoding as UTF-8; null for a `%` that does not start an escape. */
        private fun decode(segment: String): String? {
            if ('%' !in segment) return segment
            if (segment.replace(ESCAPE, "").contains('%')) return null
            val bytes = mutableListOf<Byte>()
            var index = 0
            while (index < segment.length) {
                if (segment[index] == '%') {
                    bytes += segment.substring(index + 1, index + 3).toInt(16).toByte()
                    index += 3
                } else {
                    val end = segment.indexOf('%', index).let { if (it < 0) segment.length else it }
                    bytes += segment.substring(index, end).encodeToByteArray().toList()
                    index = end
                }
            }
            return bytes.toByteArray().decodeToString()
        }
    }
}
