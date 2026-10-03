package com.servacode.directory.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.VectorPath
import com.servacode.directory.designsystem.generated.DirectoryIconPaths
import com.servacode.directory.designsystem.generated.DirectoryTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GlyphTest {
    @Test
    fun `a token's colour is read as written and opaque unless it says otherwise`() {
        val hex = DirectoryTokens.ColorsPrimary
        val digits = hex.removePrefix("#")
        val channel = { at: Int -> digits.substring(at, at + 2).toInt(16) / 255f }

        val opaque = hexColor(hex)
        assertEquals(Color(channel(0), channel(2), channel(4), 1f), opaque)
        val translucent = hexColor("#80$digits")
        assertEquals(0x80 / 255f, translucent.alpha, 0.002f)
        assertEquals(opaque.copy(alpha = translucent.alpha), translucent)
        assertFailsWith<IllegalArgumentException> { hexColor("#" + digits.take(4)) }
    }

    @Test
    fun `an icon of the shared set is its paths at the site's stroke and mirrored where it points`() {
        val back = DirectoryIcons.back.build(dark = false)

        assertTrue(back.autoMirror)
        assertEquals(DirectoryIconPaths.arrowBack.paths.size, back.root.size)
        val path = back.root[0] as VectorPath
        assertEquals(1.8f, path.strokeLineWidth)
        assertNull(path.fill)
        assertFalse(DirectoryIcons.phone.build(dark = false).autoMirror)
    }

    @Test
    fun `an untinted icon wears the theme's colours in light and dark`() {
        val light = DirectoryIcons.closeBox.build(dark = false)
        val dark = DirectoryIcons.closeBox.build(dark = true)

        assertEquals(
            SolidColor(hexColor(DirectoryTokens.SemanticFeedbackDanger)),
            (light.root[0] as VectorPath).fill,
        )
        assertEquals(
            SolidColor(hexColor(DirectoryTokens.SemanticDarkFeedbackDanger)),
            (dark.root[0] as VectorPath).fill,
        )
        assertEquals(
            SolidColor(hexColor(DirectoryTokens.SemanticDarkContentOnPrimary)),
            (dark.root[1] as VectorPath).stroke,
        )
    }

    @Test
    fun `an illustration is its soft ground then its lines then its accent in the theme's colours`() {
        val art = DirectoryIllustrations.offline
        val vector = illustrationVector(art, dark = true)
        val paths = (0 until vector.root.size).map { vector.root[it] as VectorPath }

        assertEquals(art.soft.size + art.line.size + art.accent.size, paths.size)
        assertEquals(SolidColor(hexColor(DirectoryTokens.SemanticDarkSurfaceBrandSoft)), paths.first().fill)
        assertEquals(SolidColor(hexColor(DirectoryTokens.SemanticDarkActionPrimary)), paths.last().stroke)
        assertEquals(2.5f, paths.last().strokeLineWidth)
    }

    @Test
    fun `every category the platform serves has a mark of its own`() {
        val marks = listOf("pharmacy", "hospital", "clinic", "laboratory", "nursing", "supplies")
            .map { DirectoryIcons.category(it) }

        assertEquals(marks.size, marks.toSet().size)
        assertEquals(DirectoryIcons.grid, DirectoryIcons.category("unheard-of"))
    }
}
