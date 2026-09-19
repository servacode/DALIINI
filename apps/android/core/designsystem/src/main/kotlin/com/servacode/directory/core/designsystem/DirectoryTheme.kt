package com.servacode.directory.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.servacode.directory.designsystem.generated.DirectoryTokens

private fun color(hex: String): Color = Color(android.graphics.Color.parseColor(hex))

private val DirectoryColors = lightColorScheme(
    primary = color(DirectoryTokens.SemanticActionPrimary),
    onPrimary = color(DirectoryTokens.SemanticContentOnPrimary),
    background = color(DirectoryTokens.SemanticSurfaceCanvas),
    onBackground = color(DirectoryTokens.SemanticContentPrimary),
    surface = color(DirectoryTokens.SemanticSurfaceDefault),
    onSurface = color(DirectoryTokens.SemanticContentPrimary),
    error = color(DirectoryTokens.SemanticFeedbackDanger),
)

/** Brand colours that sit outside the Material colour scheme, from the same tokens. */
object BrandColors {
    /** The splash, system and app alike (brand_splash_background). */
    val splashBackground = color(DirectoryTokens.SemanticSurfaceDefault)
    val mark = color(DirectoryTokens.SemanticActionPrimary)
    val soft = color(DirectoryTokens.SemanticSurfaceBrandSoft)
    val softer = color(DirectoryTokens.ColorsPrimarySofter)
    val contentSecondary = color(DirectoryTokens.SemanticContentSecondary)
}

/**
 * Token roles the Material typography does not carry. Kept apart from it on purpose: Material
 * components read their own roles (a navigation bar reads labelMedium), so filling those slots
 * would restyle screens that never asked for it.
 */
object DirectoryTextStyles {
    val display = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight(DirectoryTokens.TypographyRolesDisplayWeight),
        fontSize = DirectoryTokens.TypographyRolesDisplaySize.sp,
        lineHeight = DirectoryTokens.TypographyRolesDisplayLineHeight.sp,
    )
    val labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight(DirectoryTokens.TypographyRolesLabelMediumWeight),
        fontSize = DirectoryTokens.TypographyRolesLabelMediumSize.sp,
        lineHeight = DirectoryTokens.TypographyRolesLabelMediumLineHeight.sp,
    )
}

private val DirectoryTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = DirectoryTokens.TypographyRolesHeadlineLargeSize.sp,
        lineHeight = DirectoryTokens.TypographyRolesHeadlineLargeLineHeight.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = DirectoryTokens.TypographyRolesBodyLargeSize.sp,
        lineHeight = DirectoryTokens.TypographyRolesBodyLargeLineHeight.sp,
    ),
)

@Composable
fun DirectoryTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_VARIABLE") val darkModeRequested = isSystemInDarkTheme()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = DirectoryColors,
            typography = DirectoryTypography,
            content = content,
        )
    }
}
