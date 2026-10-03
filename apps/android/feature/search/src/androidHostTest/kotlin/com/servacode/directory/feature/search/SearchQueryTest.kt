package com.servacode.directory.feature.search

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchQueryTest {
    @Test fun normalizesWhitespace() {
        assertEquals("صيدلية الرحمة", SearchQuery.normalize("  صيدلية   الرحمة  "))
    }
}
