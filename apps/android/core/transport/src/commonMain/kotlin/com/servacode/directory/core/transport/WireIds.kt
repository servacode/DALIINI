package com.servacode.directory.core.transport

/*
 * The domain keeps every id as an opaque String; the backend keys most of them by UUID.
 *
 * The JVM client took `java.util.UUID` for those parameters and fields, and the Android adapters
 * built each one with `UUID.fromString` inside `call {}`. Two things followed, and both are kept
 * here although the multiplatform client takes a plain String:
 *  - an id that is not a UUID fails as UNEXPECTED before any request is made, instead of
 *    reaching the backend as a 404 or a 400;
 *  - an id goes out as the UUID's own text, lower-case 8-4-4-4-12, whatever case it came in.
 */

/**
 * [id] as the backend's UUID text, or an [IllegalArgumentException] when it is not one.
 *
 * As lenient as `UUID.fromString`, which it replaces: at most 36 characters, five groups of hex
 * digits separated by dashes, each group read as a number and kept to its slot's width, so
 * `1-2-3-4-5` is `00000001-0002-0003-0004-000000000005`. A group too large for a 64-bit number
 * fails, as `Long.parseLong` fails it.
 */
internal fun uuid(id: String): String {
    require(id.length <= UUID_TEXT_LENGTH) { "UUID string too large" }
    val groups = id.split('-')
    require(groups.size == UUID_GROUP_WIDTHS.size) { "Invalid UUID string: $id" }
    // String.toLong throws NumberFormatException, an IllegalArgumentException, as Java's does.
    return groups.mapIndexed { index, group -> group.toLong(HEX).slot(UUID_GROUP_WIDTHS[index]) }
        .joinToString("-")
}

/** The low [width] hex digits of this number, zero-padded. */
private fun Long.slot(width: Int): String =
    (this and ((1L shl (width * BITS_PER_HEX_DIGIT)) - 1)).toString(HEX).padStart(width, '0')

private const val UUID_TEXT_LENGTH = 36
private val UUID_GROUP_WIDTHS = intArrayOf(8, 4, 4, 4, 12)
private const val HEX = 16
private const val BITS_PER_HEX_DIGIT = 4
