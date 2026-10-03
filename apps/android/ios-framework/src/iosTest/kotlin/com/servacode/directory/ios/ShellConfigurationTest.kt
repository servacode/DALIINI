package com.servacode.directory.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import platform.Foundation.NSBundle

class ShellConfigurationTest {
    @Test
    fun `a bundle without the keys is not configured and never cleartext`() {
        // The test binary's own bundle carries no Info.plist keys of the app's.
        val configuration = ShellConfiguration.fromBundle(NSBundle.mainBundle)

        assertEquals(ShellConfiguration(apiBaseUrl = "", allowCleartext = false), configuration)
    }
}
