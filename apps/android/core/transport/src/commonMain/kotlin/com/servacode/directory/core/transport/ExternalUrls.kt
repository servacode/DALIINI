package com.servacode.directory.core.transport

/**
 * An absolute `https` address with a host and no credentials.
 *
 * Android asks `java.net.URI` (`isSafeExternalUrl` in its `Mappers.kt`): the address must parse,
 * its scheme must be https in any case, its authority must be a server — a host name, an IPv4
 * address or a bracketed IPv6 literal, with an optional port — and not a registry name, and it
 * must carry no user information. Common Kotlin has no URI class, and Ktor's `Url` accepts far
 * more than java.net.URI does, so the parts of java.net.URI's parser (RFC 2396 with its
 * documented deviations) this question depends on are reproduced here: an address it would
 * refuse, or would read without a server host, is refused here too.
 */
internal fun isSafeExternalUrl(value: String): Boolean = try {
    UriSyntax(value).isHttpsServerWithoutUserInfo()
} catch (_: MalformedUri) {
    false
}

/** java.net.URI would throw, or would find no server host; either way the answer is no. */
private class MalformedUri : Exception()

private class UriSyntax(private val input: String) {
    private val n = input.length
    private var ipv6Bytes = 0

    fun isHttpsServerWithoutUserInfo(): Boolean {
        // The scheme runs to the first ':' met before any '/', '?' or '#'; without one the
        // address is relative.
        val colon = scanTo(0, n, ":/?#")
        if (colon >= n || input[colon] != ':') return false
        val scheme = input.substring(0, colon)
        if (!scheme.all(::isAlpha) || !scheme.equals(HTTPS, ignoreCase = true)) return false
        var p = colon + 1
        // "https:x" is opaque and "https:/x" has no authority: neither has a host.
        if (!at(p, n, "//")) return false
        p += 2
        val authorityEnd = scanTo(p, n, "/?#")
        // "https:///x" has an empty authority, which java.net.URI accepts, and no host.
        if (authorityEnd == p || !isServerWithoutUserInfo(p, authorityEnd)) return false
        p = authorityEnd
        val pathEnd = scanTo(p, n, "?#")
        checkChars(p, pathEnd, PATH)
        p = pathEnd
        if (at(p, n, "?")) {
            val queryEnd = scanTo(p + 1, n, "#")
            checkChars(p + 1, queryEnd, URIC)
            p = queryEnd
        }
        // The fragment runs to the end, so a second '#' is refused with the rest.
        if (at(p, n, "#")) checkChars(p + 1, n, URIC)
        return true
    }

    /**
     * Whether [start, end) reads as a server-based authority with no user information.
     *
     * Any '@' means user information, or an authority that is not a server at all: either way
     * not safe. Otherwise java.net.URI's `parseServer` decides, and when it fails the authority
     * is a registry name with no host, or the address does not parse.
     */
    private fun isServerWithoutUserInfo(start: Int, end: Int): Boolean {
        if (scanTo(start, end, "@") < end) return false
        var p = if (at(start, end, "[")) {
            val close = scanTo(start + 1, end, "]")
            if (close == start + 1 || close >= end) malformed()
            // An optional "%scope" after the address.
            val percent = scanTo(start + 1, close, "%")
            if (percent > start + 1) {
                ipv6(start + 1, percent)
                if (percent + 1 == close) malformed()
                if (scanWhile(percent + 1, close, ::isScopeIdChar) < close) malformed()
            } else {
                ipv6(start + 1, close)
            }
            close + 1
        } else {
            ipv4Host(start, end).takeIf { it > start } ?: hostname(start, end)
        }
        if (at(p, end, ":")) {
            p++
            val digitsEnd = scanWhile(p, end, ::isDigit)
            if (digitsEnd > p) {
                input.substring(p, digitsEnd).toIntOrNull() ?: malformed()
                p = digitsEnd
            }
        }
        return p == end
    }

    /** `parseIPv4Address`: where a dotted-quad host ends, or -1 when there is none. */
    private fun ipv4Host(start: Int, end: Int): Int {
        val p = try {
            scanIpv4(start, end, strict = false)
        } catch (_: MalformedUri) {
            -1
        }
        // An address may be followed only by a port.
        return if (p in (start + 1) until end && input[p] != ':') -1 else p
    }

    /** `scanIPv4Address`: four bytes of at most 255, or -1 when no digits start here. */
    private fun scanIpv4(start: Int, end: Int, strict: Boolean): Int {
        val m = scanWhile(start, end) { isDigit(it) || it == '.' }
        if (m <= start || (strict && m != end)) return -1
        var p = start
        for (index in 0 until IPV4_BYTES) {
            val q = ipv4Byte(p, m)
            if (q <= p) malformed()
            p = q
            if (index < IPV4_BYTES - 1) {
                if (!at(p, m, ".")) malformed()
                p++
            }
        }
        if (p < m) malformed()
        return p
    }

    private fun ipv4Byte(start: Int, end: Int): Int {
        val q = scanWhile(start, end, ::isDigit)
        if (q <= start) return q
        val value = input.substring(start, q).toIntOrNull() ?: malformed()
        return if (value > MAX_BYTE) start else q
    }

    /**
     * `parseHostname`: labels of letters, digits and inner dashes, separated by dots, the last
     * starting with a letter when there is more than one; returns where the name ends.
     */
    private fun hostname(start: Int, end: Int): Int {
        var p = start
        var lastLabel = -1
        do {
            var q = scanWhile(p, end, ::isAlphanumeric)
            if (q <= p) break
            lastLabel = p
            p = q
            q = scanWhile(p, end) { isAlphanumeric(it) || it == '-' }
            if (q > p) {
                if (input[q - 1] == '-') malformed()
                p = q
            }
            if (!at(p, end, ".")) break
            p++
        } while (p < end)
        if (p < end && !at(p, end, ":")) malformed()
        if (lastLabel < 0) malformed()
        if (lastLabel > start && !isAlpha(input[lastLabel])) malformed()
        return p
    }

    /** `parseIPv6Reference`: all of [start, end) is one IPv6 address, or the address is refused. */
    private fun ipv6(start: Int, end: Int) {
        var p = start
        var compressedZeros = false
        val q = hexSequence(p, end)
        if (q > p) {
            p = q
            if (at(p, end, "::")) {
                compressedZeros = true
                p = hexTail(p + 2, end)
            } else if (at(p, end, ":")) {
                p = ipv4Within(p + 1, end)
                ipv6Bytes += IPV4_BYTES
            }
        } else if (at(p, end, "::")) {
            compressedZeros = true
            p = hexTail(p + 2, end)
        }
        if (p < end || ipv6Bytes > IPV6_BYTES) malformed()
        if (!compressedZeros && ipv6Bytes < IPV6_BYTES) malformed()
        if (compressedZeros && ipv6Bytes == IPV6_BYTES) malformed()
    }

    /** `scanHexPost`: what follows "::". */
    private fun hexTail(start: Int, end: Int): Int {
        if (start == end) return start
        val q = hexSequence(start, end)
        if (q > start) {
            if (!at(q, end, ":")) return q
            return ipv4Within(q + 1, end).also { ipv6Bytes += IPV4_BYTES }
        }
        return ipv4Within(start, end).also { ipv6Bytes += IPV4_BYTES }
    }

    /** `scanHexSeq`: groups of one to four hex digits, or -1 when none starts here. */
    private fun hexSequence(start: Int, end: Int): Int {
        var q = scanWhile(start, end, ::isHex)
        // Digits followed by a dot begin an embedded IPv4 address instead.
        if (q <= start || at(q, end, ".")) return -1
        if (q > start + HEX_GROUP) malformed()
        ipv6Bytes += 2
        var p = q
        while (p < end) {
            if (!at(p, end, ":") || at(p + 1, end, ":")) break
            p++
            q = scanWhile(p, end, ::isHex)
            if (q <= p) malformed()
            if (at(q, end, ".")) {
                p--
                break
            }
            if (q > p + HEX_GROUP) malformed()
            ipv6Bytes += 2
            p = q
        }
        return p
    }

    /** `takeIPv4Address`: an IPv4 address that must fill the rest of an IPv6 literal. */
    private fun ipv4Within(start: Int, end: Int): Int {
        val p = scanIpv4(start, end, strict = true)
        if (p <= start) malformed()
        return p
    }

    /**
     * `checkChars` for a component that allows escapes: each character is one of [allowed], a
     * letter, a digit, a `%` with two hex digits, or a visible character beyond US-ASCII.
     */
    private fun checkChars(start: Int, end: Int, allowed: String) {
        var p = start
        while (p < end) {
            val c = input[p]
            p += when {
                isAlphanumeric(c) || c in allowed -> 1
                c == '%' && p + ESCAPE_LENGTH <= end && isHex(input[p + 1]) && isHex(input[p + 2]) -> ESCAPE_LENGTH
                c.code > LAST_EXCLUDED_CODE && !c.isSpaceChar() && !c.isISOControl() -> 1
                else -> malformed()
            }
        }
    }

    private fun scanTo(start: Int, end: Int, stops: String): Int {
        var p = start
        while (p < end && input[p] !in stops) p++
        return p
    }

    private fun scanWhile(start: Int, end: Int, accept: (Char) -> Boolean): Int {
        var p = start
        while (p < end && accept(input[p])) p++
        return p
    }

    private fun at(start: Int, end: Int, text: String): Boolean =
        start >= 0 && text.length <= end - start && input.startsWith(text, start)

    private fun malformed(): Nothing = throw MalformedUri()

    private companion object {
        const val HTTPS = "https"
        const val MARK = "-_.!~*'()"
        const val PATH = "$MARK:@&=+$,;/"
        const val URIC = "$MARK;/?:@&=+$,[]"
        const val IPV4_BYTES = 4
        const val IPV6_BYTES = 16
        const val MAX_BYTE = 255
        const val HEX_GROUP = 4
        const val ESCAPE_LENGTH = 3

        // java.net.URI lets "other" characters through from U+0081 up, not U+0080 itself.
        const val LAST_EXCLUDED_CODE = 128
    }
}

private fun isAlpha(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z'

private fun isDigit(c: Char): Boolean = c in '0'..'9'

private fun isAlphanumeric(c: Char): Boolean = isAlpha(c) || isDigit(c)

private fun isHex(c: Char): Boolean = isDigit(c) || c in 'a'..'f' || c in 'A'..'F'

private fun isScopeIdChar(c: Char): Boolean = isAlphanumeric(c) || c == '_' || c == '.'

/** `Character.isSpaceChar`: a space, line or paragraph separator. */
private fun Char.isSpaceChar(): Boolean = category == CharCategory.SPACE_SEPARATOR ||
    category == CharCategory.LINE_SEPARATOR ||
    category == CharCategory.PARAGRAPH_SEPARATOR
