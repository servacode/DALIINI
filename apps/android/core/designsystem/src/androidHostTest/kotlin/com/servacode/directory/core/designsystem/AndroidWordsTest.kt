package com.servacode.directory.core.designsystem

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The few words Android reads by id outside Compose are copies of the shared ones; these hold
 * them to each other, and hold the shared words to what Android's own resources would show.
 */
class AndroidWordsTest {
    private val pattern = Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)

    private fun strings(path: String): Map<String, String> =
        pattern.findAll(File(path).readText()).associate { it.groupValues[1] to it.groupValues[2] }

    private val shared = strings("src/commonMain/composeResources/values/strings.xml")

    @Test
    fun `android's copies say what the shared words say`() {
        val android = strings("src/androidMain/res/values/strings.xml")

        assertTrue(android.isNotEmpty())
        android.forEach { (name, value) ->
            // Android trims a string's ends unless it is quoted; the shared words keep them.
            assertEquals(name, shared[name], value.removeSurrounding("\""))
        }
    }

    @Test
    fun `no shared word carries the layout of the file it is written in`() {
        // Android collapses a line break and its indentation into a space; Compose resources keep
        // them, so a sentence wrapped in the file would be wrapped on screen.
        shared.forEach { (name, value) ->
            assertTrue(name, '\n' !in value && "  " !in value)
            if (name != "directory_list_separator") assertEquals(name, value.trim(), value)
        }
    }
}
