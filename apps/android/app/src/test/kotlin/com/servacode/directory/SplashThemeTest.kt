package com.servacode.directory

import com.servacode.directory.core.designsystem.BrandMarkCanvas
import com.servacode.directory.core.designsystem.BrandMarkSize
import com.servacode.directory.designsystem.generated.DirectoryTokens
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The system splash and the app's splash are two pieces of UI that must look like one: the
 * system shows the first, the app draws the second, and a difference between them is a flash.
 * These read the resources as the build ships them and hold them to the same tokens.
 */
class SplashThemeTest {
    private val android = "http://schemas.android.com/apk/res/android"

    private fun document(path: String) = DocumentBuilderFactory.newInstance()
        .apply { isNamespaceAware = true }
        .newDocumentBuilder()
        .parse(File(path))

    private fun style(name: String): Element {
        val styles = document("src/main/res/values/styles.xml").getElementsByTagName("style")
        return (0 until styles.length).map { styles.item(it) as Element }.single { it.getAttribute("name") == name }
    }

    private fun Element.item(name: String): String {
        val items = getElementsByTagName("item")
        return (0 until items.length).map { items.item(it) as Element }
            .single { it.getAttribute("name") == name }.textContent.trim()
    }

    private fun dp(value: String): Float = value.removeSuffix("dp").toFloat()

    @Test fun `the launcher activity starts in the splash theme and moves on to the app theme`() {
        val activities = document("src/main/AndroidManifest.xml").getElementsByTagName("activity")
        val main = (0 until activities.length).map { activities.item(it) as Element }
            .single { it.getAttributeNS(android, "name") == ".MainActivity" }

        assertEquals("@style/Theme.Directory.Starting", main.getAttributeNS(android, "theme"))
        assertEquals("Theme.SplashScreen", style("Theme.Directory.Starting").getAttribute("parent"))
        assertEquals("@style/Theme.Directory", style("Theme.Directory.Starting").item("postSplashScreenTheme"))
    }

    private fun colour(path: String, name: String): String {
        val colours = document(path).getElementsByTagName("color")
        return (0 until colours.length).map { colours.item(it) as Element }
            .single { it.getAttribute("name") == name }.textContent.trim()
    }

    @Test fun `both splashes have the one background colour, the token the app's splash paints`() {
        assertEquals("@color/brand_splash_background", style("Theme.Directory.Starting").item("windowSplashScreenBackground"))
        val alias = colour("../core/designsystem/src/main/res/values/brand.xml", "brand_splash_background")
        assertEquals("@color/token_semantic_surface_default", alias)
        val value = colour(
            "../../../packages/design-tokens/generated/android/values/directory_token_colors.xml",
            "token_semantic_surface_default",
        )

        // BrandColors.splashBackground, which the app's splash paints, is this same token.
        assertEquals(DirectoryTokens.SemanticSurfaceDefault.uppercase(), value.uppercase())
    }

    @Test fun `both splashes draw one mark file, the mark in the middle of its canvas`() {
        assertEquals("@drawable/brand_mark", style("Theme.Directory.Starting").item("windowSplashScreenAnimatedIcon"))
        val mark = document("../core/designsystem/src/main/res/drawable/brand_mark.xml").documentElement
        val canvas = dp(mark.getAttributeNS(android, "width"))
        val group = mark.getElementsByTagName("group").item(0) as Element

        // The app's splash draws the whole canvas at BrandMarkCanvas, like the system splash.
        assertEquals(BrandMarkCanvas.value, canvas)
        assertEquals(canvas, mark.getAttributeNS(android, "viewportWidth").toFloat())
        // The mark is a margin-free 120 units, centred: proportional at any size it is drawn.
        assertEquals(group.getAttributeNS(android, "translateX"), group.getAttributeNS(android, "translateY"))
        assertEquals(BrandMarkSize.value, canvas - 2 * group.getAttributeNS(android, "translateX").toFloat())
    }
}
