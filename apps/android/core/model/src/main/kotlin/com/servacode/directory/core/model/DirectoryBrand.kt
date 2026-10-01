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
    /**
     * A name, not a translation.
     *
     * It stays a constant where every other word of the app moved to resources, because it is a
     * proper noun: it is the same in any language the app is read in, and it must match the
     * launcher's label, which the manifest reads from `app_name`. `BrandNameTest` holds the two
     * together. What the app is *for* is a sentence, and that lives in the design system's
     * resources as `DirectoryWords.TAGLINE`.
     */
    const val NAME = "دليني"
}
