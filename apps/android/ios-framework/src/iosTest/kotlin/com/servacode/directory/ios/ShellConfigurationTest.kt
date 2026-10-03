package com.servacode.directory.ios

import platform.Foundation.NSBundle
import kotlin.test.Test
import kotlin.test.assertEquals

class ShellConfigurationTest {
    @Test
    fun `a bundle without the keys is not configured and never cleartext`() {
        // The test binary's own bundle carries no Info.plist keys of the app's.
        val configuration = ShellConfiguration.fromBundle(NSBundle.mainBundle)

        assertEquals(ShellConfiguration(apiBaseUrl = "", allowCleartext = false), configuration)
    }
}
