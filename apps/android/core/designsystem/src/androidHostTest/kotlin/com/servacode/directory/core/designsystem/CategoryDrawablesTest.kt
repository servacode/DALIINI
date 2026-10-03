package com.servacode.directory.core.designsystem

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The map draws a category's mark from an Android drawable; every other screen from the shared
 * glyph. These hold the two to the same drawing, key by key.
 */
class CategoryDrawablesTest {
    private val pathData = Regex("""android:pathData="([^"]+)"""")

    private fun drawable(name: String): List<String> {
        val local = File("src/androidMain/res/drawable/$name.xml")
        val shared = File("../../../../packages/design-tokens/generated/android/drawable/$name.xml")
        val file = if (local.exists()) local else shared
        return pathData.findAll(file.readText()).map { it.groupValues[1] }.toList()
    }

    private val expected = mapOf(
        "pharmacy" to ("dl_ic_pharmacy" to R.drawable.dl_ic_pharmacy),
        "hospital" to ("ic_hospital" to R.drawable.ic_hospital),
        "clinic" to ("dl_ic_clinic" to R.drawable.dl_ic_clinic),
        "medical-clinic" to ("dl_ic_clinic" to R.drawable.dl_ic_clinic),
        "laboratory" to ("dl_ic_lab" to R.drawable.dl_ic_lab),
        "medical-laboratory" to ("dl_ic_lab" to R.drawable.dl_ic_lab),
        "nursing" to ("ic_nursing" to R.drawable.ic_nursing),
        "nursing-center" to ("ic_nursing" to R.drawable.ic_nursing),
        "supplies" to ("ic_supplies" to R.drawable.ic_supplies),
        "medical-supplies" to ("ic_supplies" to R.drawable.ic_supplies),
        "something-new" to ("dl_ic_grid" to R.drawable.dl_ic_grid),
        null to ("dl_ic_grid" to R.drawable.dl_ic_grid),
    )

    @Test
    fun `the map's pin and the screens' icon are the same drawing for every category`() {
        expected.forEach { (key, drawable) ->
            val (name, id) = drawable
            assertEquals("$key", id, CategoryDrawables.of(key))
            assertEquals("$key", drawable(name), DirectoryIcons.category(key).paths.map { it.data })
        }
    }
}
