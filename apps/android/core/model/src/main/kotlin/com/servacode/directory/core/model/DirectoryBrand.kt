package com.servacode.directory.core.model

/**
 * What this app is called — in one place, because it was in five.
 *
 * The name was written as a literal in the manifest, in the notification builder, on the splash,
 * on Home's header and on the sign-in page. Three said one thing and two said another, and none
 * of them said the name.
 *
 * It lives here rather than with the mark because a name is a fact about the product, not a
 * visual token: the copy that reads it is compiled without the Android framework, and the design
 * system is not.
 */
object DirectoryBrand {
    const val NAME = "دليني"

    /** What the app is for, under its name where there is room for it. */
    const val TAGLINE = "أقرب الخدمات الصحية إليك"
}
