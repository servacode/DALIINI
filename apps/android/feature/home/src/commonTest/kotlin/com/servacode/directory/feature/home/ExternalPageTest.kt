package com.servacode.directory.feature.home

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

/** An advertisement opens a page only over https and only with a host, on both platforms. */
class ExternalPageTest {
    @Test fun `an https page with a host is opened`() {
        assertTrue(isExternalPage("https://example.com"))
        assertTrue(isExternalPage("https://example.com/offer?id=1#top"))
        assertTrue(isExternalPage("https://user@example.com:8443/x"))
    }

    @Test fun `anything else is not`() {
        assertFalse(isExternalPage("http://example.com"))
        assertFalse(isExternalPage("HTTPS://example.com"))
        assertFalse(isExternalPage("https://"))
        assertFalse(isExternalPage("https:///path"))
        assertFalse(isExternalPage("javascript:alert(1)"))
        assertFalse(isExternalPage("intent://scan/#Intent;scheme=zxing;end"))
    }
}
